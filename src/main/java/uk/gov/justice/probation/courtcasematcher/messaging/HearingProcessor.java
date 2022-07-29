package uk.gov.justice.probation.courtcasematcher.messaging;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.mapper.HearingMapper;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.justice.probation.courtcasematcher.messaging.IncomingHearingComparator.hasCourtHearingChanged;

@AllArgsConstructor(onConstructor_ = @Autowired)
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Service
@Qualifier("caseMessageProcessor")
@Slf4j
public class HearingProcessor {

    @NonNull
    private final TelemetryService telemetryService;

    @NonNull
    private final CourtCaseService courtCaseService;

    @NonNull
    private final MatcherService matcherService;

    public void process(Hearing receivedHearing, String messageId) {
        try {
            // New LIBRA cases will have no case or defendant ID, and we need to assign
            if (receivedHearing.getSource() == DataSource.LIBRA && receivedHearing.getCaseId() == null) {
                receivedHearing = assignUuids(receivedHearing);
            }
            matchAndSaveCase(receivedHearing, messageId);
        } catch (Exception ex) {
            log.error("Message processing failed. Error: {} ", ex.getMessage(), ex);
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private void matchAndSaveCase(Hearing receivedHearing, String messageId) {

        courtCaseService.findHearing(receivedHearing)
                .blockOptional()
                .ifPresentOrElse(
                        existingCourtCase -> {
                            if (hasCourtHearingChanged(receivedHearing, existingCourtCase)) {
                                telemetryService.trackHearingChangedEvent(receivedHearing);
                                mergeAndUpdateExistingCase(receivedHearing, existingCourtCase);
                            } else {
                                telemetryService.trackHearingUnChangedEvent(receivedHearing);
                            }
                        },
                        () -> {
                            telemetryService.trackHearingEvent(receivedHearing, messageId);
                            applyMatchesAndSave(receivedHearing);
                        }
                );
    }

    private void mergeAndUpdateExistingCase(Hearing receivedHearing, Hearing existingHearing) {
        var courtCaseMerged = HearingMapper.merge(receivedHearing, existingHearing);
        updateAndSave(courtCaseMerged);
    }


    private void applyMatchesAndSave(final Hearing hearing) {
        matcherService.matchDefendants(hearing)
                .onErrorReturn(hearing)
                .doOnSuccess(courtCaseService::saveHearing)
                .block();
    }

    private void updateAndSave(final Hearing hearing) {
        log.info("Upsert caseId {}", hearing.getCaseId());

        courtCaseService.updateProbationStatusDetail(hearing)
                .onErrorResume(t -> Mono.just(hearing))
                .subscribe(courtCaseService::saveHearing);
    }

    Hearing assignUuids(Hearing hearing) {
        // Apply the new case ID
        final var caseId = UUID.randomUUID().toString();
        var updatedCase = hearing.withCaseId(caseId).withHearingId(caseId);

        // We want to retain the LIBRA case no if present
        if (hearing.getCaseNo() == null) {
            updatedCase = updatedCase.withCaseNo(caseId);
        }

        // Assign defendant IDs
        final var updatedDefendants = hearing.getDefendants()
                .stream()
                .map(defendant -> defendant.withDefendantId(
                        defendant.getDefendantId() == null ? UUID.randomUUID().toString() : defendant.getDefendantId()
                ))
                .collect(Collectors.toList());
        return updatedCase.withDefendants(updatedDefendants);

    }
}