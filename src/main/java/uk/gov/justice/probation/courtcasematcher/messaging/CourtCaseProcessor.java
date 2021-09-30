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
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Service
@Qualifier("caseMessageProcessor")
@Slf4j
public class CourtCaseProcessor {

    @Autowired
    @NonNull
    private final TelemetryService telemetryService;

    @Autowired
    @NonNull
    private final CourtCaseService courtCaseService;

    @Autowired
    @NonNull
    private final MatcherService matcherService;

    public void process(CourtCase courtCase, String messageId) {
        try {
            matchAndSaveCase(courtCase, messageId);
        }
        catch (Exception ex) {
            log.error("Message processing failed. Error: {} ", ex.getMessage(), ex);
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private void matchAndSaveCase(CourtCase aCase, String messageId) {
        telemetryService.trackCourtCaseEvent(aCase, messageId);
        final var courtCase = courtCaseService.getCourtCase(aCase)
                .block();
        if (courtCase.shouldMatchToOffender()) {
            applyMatches(courtCase);
        }
        else {
            updateAndSave(courtCase);
        }
    }


    private void applyMatches(final CourtCase courtCase) {
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
}
