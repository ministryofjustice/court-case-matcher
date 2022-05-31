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

import java.util.Objects;

import static java.util.Objects.requireNonNull;

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

    public void process(CourtCase courtCaseReceived, String messageId) {
        try {
            matchAndSaveCase(courtCaseReceived, messageId);
        }
        catch (Exception ex) {
            log.error("Message processing failed. Error: {} ", ex.getMessage(), ex);
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private void matchAndSaveCase(CourtCase courtCaseReceived, String messageId) {
        telemetryService.trackCourtCaseEvent(courtCaseReceived, messageId);
        final var courtCaseMerged = courtCaseService.getCourtCase(courtCaseReceived)
                .block();
        if(!requireNonNull(courtCaseMerged).equals(courtCaseReceived)) {
            if (courtCaseMerged.shouldMatchToOffender()) {
                applyMatchesAndSave(courtCaseMerged);
            } else {
                updateAndSave(courtCaseMerged);
            }
        }else{
            //TODO telemetryService
        }
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
}
