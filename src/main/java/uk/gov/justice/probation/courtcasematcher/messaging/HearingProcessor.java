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
import uk.gov.justice.probation.courtcasematcher.application.FeatureFlags;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.mapper.HearingMapper;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.CprService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.justice.probation.courtcasematcher.messaging.IncomingHearingComparator.hasCourtHearingChanged;

@AllArgsConstructor(onConstructor_ = @Autowired)
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Service
@Qualifier("caseMessageProcessor")
@Slf4j
public class HearingProcessor {

    private static final String ENRICHMENT_WORKER_FLAG = "enrichment_worker";
    private static final String ENRICHMENT_WORKER_COURT_CODE = "B03DE";

    @NonNull
    private final TelemetryService telemetryService;

    @NonNull
    private final CourtCaseService courtCaseService;


    @NonNull
    private final CprService cprService;

    private final FeatureFlags featureFlags;

    public void process(Hearing receivedHearing, String messageId) {
        try {
            // New LIBRA cases will have no case or defendant ID, and we need to assign
            if (receivedHearing.getSource() == DataSource.LIBRA && receivedHearing.getCaseId() == null) {
                receivedHearing = assignUuids(receivedHearing);
            }
            matchAndSaveHearing(receivedHearing, messageId);
        } catch (Exception ex) {
            log.error("Message processing failed.", ex);
            telemetryService.trackProcessingFailureEvent(receivedHearing);
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private void matchAndSaveHearing(Hearing receivedHearing, String messageId) {
        courtCaseService.findHearing(receivedHearing)
                .blockOptional()
                .ifPresentOrElse(
                        existingHearing -> {
                            if (hasCourtHearingChanged(receivedHearing, existingHearing)) {
                                telemetryService.trackHearingChangedEvent(receivedHearing);
                                mergeAndUpdateExistingHearing(receivedHearing, existingHearing);
                            } else {
                                telemetryService.trackHearingUnChangedEvent(receivedHearing);
                            }
                        },
                        () -> {
                            telemetryService.trackNewHearingEvent(receivedHearing, messageId);
                            applyMatchesAndSave(receivedHearing);
                        }
                );
    }

    private void mergeAndUpdateExistingHearing(Hearing receivedHearing, Hearing existingHearing) {
        var courtCaseMerged = HearingMapper.merge(receivedHearing, existingHearing);
        updateAndSave(courtCaseMerged);
    }

    private void applyMatchesAndSave(final Hearing hearing) {
        enrichAndSave(hearing, false);
    }

    private void updateAndSave(final Hearing hearing) {
        log.info("Upsert caseId {}", hearing.getCaseId());
        enrichAndSave(hearing, true);
    }

    private void enrichAndSave(final Hearing hearing, boolean updateProbationStatus) {
        boolean hasEnrichmentWorkerCourtDay = Optional.ofNullable(hearing)
                .map(Hearing::getHearingDays)
                .map(days -> days.stream()
                        .filter(Objects::nonNull)
                        .anyMatch(day -> ENRICHMENT_WORKER_COURT_CODE.equals(day.getCourtCode())))
                .orElse(false);
        if (featureFlags.getFlag(ENRICHMENT_WORKER_FLAG) && hasEnrichmentWorkerCourtDay) {
            log.info("Skipping CPR service and probation-status update for caseId {} at court {}; enrichment worker endpoint is not yet implemented",
                    hearing.getCaseId(), ENRICHMENT_WORKER_COURT_CODE);
            courtCaseService.saveHearing(hearing);
        } else {
            cprService.updateDefendants(hearing.getDefendants());
            if (updateProbationStatus) {
                courtCaseService.updateProbationStatusDetail(hearing)
                        .onErrorResume(t -> Mono.just(hearing))
                        .subscribe(courtCaseService::saveHearing);
            } else {
                courtCaseService.saveHearing(hearing);
            }
        }
    }

    Hearing assignUuids(Hearing hearing) {
        // Apply the new case ID
        final var caseId = UUID.randomUUID().toString();
        var updatedHearing = hearing.withCaseId(caseId).withHearingId(caseId);

        // We want to retain the LIBRA case no if present
        if (hearing.getCaseNo() == null) {
            updatedHearing = updatedHearing.withCaseNo(caseId);
        }

        // Assign defendant IDs
        final var updatedDefendants = hearing.getDefendants()
                .stream()
                .map(defendant -> defendant.withDefendantId(
                        defendant.getDefendantId() == null ? UUID.randomUUID().toString() : defendant.getDefendantId()
                ))
                .collect(Collectors.toList());

        return updatedHearing.withDefendants(updatedDefendants);

    }
}
