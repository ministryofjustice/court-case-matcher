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
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OffenderSearchMatchType;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.SearchResult;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import java.util.Collections;
import java.util.Optional;

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
            applyMatch(courtCase);
        }
        else {
            updateAndSave(courtCase);
        }
    }


    private void applyMatch(final CourtCase courtCase) {
        // TODO: Stream over defendants, add matches to defendant, pass to court case service for creation

        log.info("Matching offender and saving case no {} for court {}, pnc {}", courtCase.getCaseNo(), courtCase.getCourtCode(), courtCase.getFirstDefendant().getPnc());

        // TODO: Move responsibility for matching down into matcher service, rename method to enrichWithMatches or similar
        final var searchResult = matcherService.getSearchResponse(courtCase)
                .doOnSuccess(result -> telemetryService.trackOffenderMatchEvent(courtCase, result.getMatchResponse()))
                .doOnError(throwable -> {
                    log.error(throwable.getMessage());
                    telemetryService.trackOffenderMatchFailureEvent(courtCase);
                })
                .onErrorResume(throwable -> Mono.just(SearchResult.builder()
                        .matchResponse(
                                MatchResponse.builder()
                                        .matchedBy(OffenderSearchMatchType.NOTHING)
                                        .matches(Collections.emptyList())
                                        .build())
                        .build()))
                .block();

        courtCaseService.createCase(courtCase, searchResult);
    }

    private void updateAndSave(final CourtCase courtCase) {
        log.info("Upsert case no {} with crn {} for court {}", courtCase.getCaseNo(), courtCase.getFirstDefendant().getCrn(), courtCase.getCourtCode());

        Optional.ofNullable(courtCase.getFirstDefendant().getCrn())
            .map(crn -> courtCaseService.updateProbationStatusDetail(courtCase)
                .onErrorReturn(courtCase))
            .orElse(Mono.just(courtCase))
            .subscribe(courtCaseService::saveCourtCase);
    }
}
