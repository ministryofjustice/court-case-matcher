package uk.gov.justice.probation.courtcasematcher.messaging;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.application.FeatureFlags;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.mapper.HearingMapper;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.CprService;
import uk.gov.justice.probation.courtcasematcher.service.EnrichmentWorkerService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.justice.probation.courtcasematcher.messaging.IncomingHearingComparator.hasCourtHearingChanged;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Service
@Qualifier("caseMessageProcessor")
@Slf4j
public class HearingProcessor {

    private static final String ENRICHMENT_WORKER_FLAG = "enrichment_worker";

    @NonNull
    private final TelemetryService telemetryService;

    @NonNull
    private final CourtCaseService courtCaseService;


    @NonNull
    private final CprService cprService;

    @NonNull
    private final EnrichmentWorkerService enrichmentWorkerService;

    private final FeatureFlags featureFlags;

    private List<String> enrichmentWorkerCourtCodes;

    @Value("${enrichment-worker.court-codes}")
    void setEnrichmentWorkerCourtCodes(List<String> enrichmentWorkerCourtCodes) {
        this.enrichmentWorkerCourtCodes = enrichmentWorkerCourtCodes;
    }

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
        if (shouldUseEnrichmentWorker(hearing)) {
            getEnrichmentWorkerCourtCode(hearing)
                    .ifPresent(courtCode -> log.info("Using enrichment worker for caseId {} at court {}",
                            hearing.getCaseId(), courtCode));
            enrichmentWorkerService.updateDefendants(hearing.getDefendants(), hearing.getSource());
        } else {
            cprService.updateDefendants(hearing.getDefendants());
        }
        courtCaseService.saveHearing(hearing);
    }

    private void updateAndSave(final Hearing hearing) {
        log.info("Upsert caseId {}", hearing.getCaseId());
        if (shouldUseEnrichmentWorker(hearing)) {
            getEnrichmentWorkerCourtCode(hearing)
                    .ifPresent(courtCode -> log.info("Using enrichment worker for caseId {} at court {}",
                            hearing.getCaseId(), courtCode));
            enrichmentWorkerService.updateDefendants(hearing.getDefendants(), hearing.getSource());
            courtCaseService.saveHearing(hearing);
        } else {
            cprService.updateDefendants(hearing.getDefendants());

            courtCaseService.updateProbationStatusDetail(hearing)
                    .onErrorResume(t -> Mono.just(hearing))
                    .subscribe(courtCaseService::saveHearing);
        }
    }

    private boolean shouldUseEnrichmentWorker(final Hearing hearing) {
        return featureFlags.getFlag(ENRICHMENT_WORKER_FLAG) && getEnrichmentWorkerCourtCode(hearing).isPresent();
    }

    private Optional<String> getEnrichmentWorkerCourtCode(final Hearing hearing) {
        return Optional.ofNullable(hearing)
                .map(Hearing::getHearingDays)
                .orElseGet(List::of)
                .stream()
                .filter(Objects::nonNull)
                .map(day -> day.getCourtCode())
                .filter(Objects::nonNull)
                .filter(enrichmentWorkerCourtCodes::contains)
                .findFirst();
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
