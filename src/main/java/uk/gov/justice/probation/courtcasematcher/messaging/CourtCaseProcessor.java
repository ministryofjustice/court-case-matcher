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

import static java.util.Objects.isNull;
import static uk.gov.justice.probation.courtcasematcher.messaging.CourtCaseComparator.hasCourtCaseChanged;

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
        } catch (Exception ex) {
            log.error("Message processing failed. Error: {} ", ex.getMessage(), ex);
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private void matchAndSaveCase(CourtCase courtCaseReceived, String messageId) {
        telemetryService.trackCourtCaseEvent(courtCaseReceived, messageId);
        final var existingCourtCase = courtCaseService.findCourtCase(courtCaseReceived)
                .block();
        if (isNull(existingCourtCase)) {
            applyMatchesAndSave(courtCaseReceived);
        } else if (hasCourtCaseChanged(courtCaseReceived, existingCourtCase)) {
            var courtCaseMerged = CaseMapper.merge(courtCaseReceived, existingCourtCase);
            updateAndSave(courtCaseMerged);
        } else {
            //TODO implement correct telemetryService
           // telemetryService.trackCourtCaseEvent(courtCaseReceived, messageId);
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
