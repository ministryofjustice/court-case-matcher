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
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.justice.probation.courtcasematcher.messaging.IncomingCourtCaseComparator.hasCourtCaseChanged;

@AllArgsConstructor(onConstructor_ = @Autowired)
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Service
@Qualifier("caseMessageProcessor")
@Slf4j
public class CourtCaseProcessor {

    @NonNull
    private final TelemetryService telemetryService;

    @NonNull
    private final CourtCaseService courtCaseService;

    @NonNull
    private final MatcherService matcherService;

    public void process(CourtCase receivedCourtCase, String messageId) {
        try {
            // New LIBRA cases will have no case or defendant ID and we need to assign
            receivedCourtCase = assignUuids(receivedCourtCase);
            matchAndSaveCase(receivedCourtCase, messageId);
        } catch (Exception ex) {
            log.error("Message processing failed. Error: {} ", ex.getMessage(), ex);
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private void matchAndSaveCase(CourtCase receivedCourtCase, String messageId) {

        courtCaseService.findCourtCase(receivedCourtCase)
                .blockOptional()
                .ifPresentOrElse(
                        existingCourtCase -> {
                            if (hasCourtCaseChanged(receivedCourtCase, existingCourtCase)) {
                                telemetryService.trackHearingChangedEvent(receivedCourtCase);
                                mergeAndUpdateExistingCase(receivedCourtCase, existingCourtCase);
                            } else {
                                telemetryService.trackHearingUnChangedEvent(receivedCourtCase);
                            }
                        },
                        () -> {
                            telemetryService.trackCourtCaseEvent(receivedCourtCase, messageId);
                            applyMatchesAndSave(receivedCourtCase);
                        }
                );
    }

    private void mergeAndUpdateExistingCase(CourtCase receivedCourtCase, CourtCase existingCourtCase) {
        var courtCaseMerged = CaseMapper.merge(receivedCourtCase, existingCourtCase);
        updateAndSave(courtCaseMerged);
    }


    private void applyMatchesAndSave(final CourtCase courtCase) {
        matcherService.matchDefendants(courtCase)
                .onErrorReturn(courtCase)
                .doOnSuccess(courtCaseService::saveCourtCase)
                .block();
    }

    private void updateAndSave(final CourtCase courtCase) {
        log.info("Upsert caseId {}", courtCase.getCaseId());

        courtCaseService.updateProbationStatusDetail(courtCase)
                .onErrorResume(t -> Mono.just(courtCase))
                .subscribe(courtCaseService::saveCourtCase);
    }

    CourtCase assignUuids(CourtCase courtCase) {
        // Apply the new case ID
        final var caseId = UUID.randomUUID().toString();
        var updatedCase = courtCase.withCaseId(caseId).withHearingId(caseId);

        // We want to retain the LIBRA case no if present
        if (courtCase.getCaseNo() == null) {
            updatedCase = updatedCase.withCaseNo(caseId);
        }

        // Assign defendant IDs
        final var updatedDefendants = courtCase.getDefendants()
                .stream()
                .map(defendant -> defendant.withDefendantId(
                        defendant.getDefendantId() == null ? UUID.randomUUID().toString() : defendant.getDefendantId()
                ))
                .collect(Collectors.toList());
        return updatedCase.withDefendants(updatedDefendants);

    }
}
