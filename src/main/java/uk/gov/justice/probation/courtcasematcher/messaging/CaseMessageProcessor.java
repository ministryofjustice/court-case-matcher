package uk.gov.justice.probation.courtcasematcher.messaging;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OffenderSearchMatchType;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.SearchResult;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import javax.validation.ConstraintViolationException;
import java.util.Collections;
import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Service
@Qualifier("caseMessageProcessor")
@Slf4j
public class CaseMessageProcessor {

    @Autowired
    private final TelemetryService telemetryService;

    @Autowired
    private final CourtCaseService courtCaseService;

    @Autowired
    private final MatcherService matcherService;

    @Autowired
    private final CaseExtractor caseExtractor;

    public void process(String payload, String messageId) {
        try {
            final CourtCase courtCase = caseExtractor.extractCourtCase(payload, messageId);
            matchAndSaveCase(courtCase, messageId);
        }
        catch (Exception ex) {
            logAndRethrow(payload, ex);
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

    private void logAndRethrow(String payload, Exception ex) {
        var failEvent = buildFailureEvent(ex, payload);
        log.error("Message processing failed. Error: {} ", failEvent.getFailureMessage(), failEvent.getThrowable());
        if (!CollectionUtils.isEmpty(failEvent.getViolations())) {
            failEvent.getViolations().forEach(
                cv -> log.error("Validation failed : {} at {} ", cv.getMessage(), cv.getPropertyPath().toString())
            );
        }
        throw new RuntimeException(failEvent.getFailureMessage(), ex);
    }


    private void applyMatch(final CourtCase courtCase) {

        log.info("Matching offender and saving case no {} for court {}, pnc {}", courtCase.getCaseNo(), courtCase.getCourtCode(), courtCase.getPnc());

        final var searchResult = matcherService.getSearchResponse(courtCase)
                .doOnSuccess(result -> telemetryService.trackOffenderMatchEvent(courtCase, result.getMatchResponse()))
                .doOnError(throwable -> telemetryService.trackOffenderMatchFailureEvent(courtCase))
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
        log.info("Upsert case no {} with crn {} for court {}", courtCase.getCaseNo(), courtCase.getCrn(), courtCase.getCourtCode());

        Optional.ofNullable(courtCase.getCrn())
            .map(crn -> courtCaseService.updateProbationStatusDetail(courtCase)
                .onErrorReturn(courtCase))
            .orElse(Mono.just(courtCase))
            .subscribe(courtCaseService::saveCourtCase);
    }

    private CourtCaseFailureEvent buildFailureEvent(Exception ex, String payload) {
        CourtCaseFailureEvent.CourtCaseFailureEventBuilder builder = CourtCaseFailureEvent.builder()
            .failureMessage(ex.getMessage())
            .throwable(ex)
            .incomingMessage(payload);
        if (ex instanceof ConstraintViolationException) {
            builder.violations(((ConstraintViolationException)ex).getConstraintViolations());
        }
        return builder.build();
    }
}
