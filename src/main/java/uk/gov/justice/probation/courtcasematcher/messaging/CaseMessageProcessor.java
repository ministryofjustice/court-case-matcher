package uk.gov.justice.probation.courtcasematcher.messaging;

import java.util.Collections;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.SnsMessageContainer;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchResponse;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.OffenderSearchMatchType;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;
import uk.gov.justice.probation.courtcasematcher.service.SearchResult;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Service
@Qualifier("caseMessageProcessor")
@Slf4j
public class CaseMessageProcessor implements MessageProcessor {

    @Autowired
    private final TelemetryService telemetryService;

    @Autowired
    private final CourtCaseService courtCaseService;

    @Autowired
    private final MatcherService matcherService;

    @Autowired
    @Qualifier("caseJsonParser")
    private final MessageParser<Case> parser;

    @Autowired
    @Qualifier("snsMessageWrapperJsonParser")
    private final MessageParser<SnsMessageContainer> snsMessageWrapperJsonParser;

    @Override
    public void process(String payload, String messageId) {
        try {
            var snsMessageContainer = extractMessage(payload);
            log.debug("Extracted message ID {} from SNS message. Incoming message ID was {} ", snsMessageContainer.getMessageId(), messageId);
            saveCase(parser.parseMessage(snsMessageContainer.getMessage(), Case.class), messageId);
        }
        catch (Exception ex) {
            var failEvent = handleException(ex, payload);
            logErrors(log, failEvent);
            throw new RuntimeException(failEvent.getFailureMessage(), ex);
        }
    }

    public void postCaseEvent(final CourtCase courtCase) {
        if (courtCase.shouldMatchToOffender()) {
            applyMatch(courtCase);
        }
        else {
            updateAndSave(courtCase);
        }
    }

    SnsMessageContainer extractMessage(String snsMessageContainer) {
        try {
            return snsMessageWrapperJsonParser.parseMessage(snsMessageContainer, SnsMessageContainer.class);
        }
        catch (Exception ex) {
            var failEvent = handleException(ex, snsMessageContainer);
            logErrors(log, failEvent);
            throw new RuntimeException(failEvent.getFailureMessage(), ex);
        }
    }

    private void applyMatch(final CourtCase courtCase) {

        log.info("Matching offender and saving case no {} for court {}, pnc {}", courtCase.getCaseNo(), courtCase.getCourtCode(), courtCase.getPnc());

        matcherService.getSearchResponse(courtCase)
            .doOnSuccess(result -> telemetryService.trackOffenderMatchEvent(courtCase, result.getMatchResponse()))
            .doOnError(throwable -> telemetryService.trackOffenderMatchFailureEvent(courtCase))
            .onErrorResume(throwable -> Mono.just(SearchResult.builder()
                .matchResponse(
                    MatchResponse.builder()
                        .matchedBy(OffenderSearchMatchType.NOTHING)
                        .matches(Collections.emptyList())
                        .build())
                .build()))
            .subscribe(searchResult -> courtCaseService.createCase(courtCase, searchResult));
    }

    private void updateAndSave(final CourtCase courtCase) {
        log.info("Upsert case no {} with crn {} for court {}", courtCase.getCaseNo(), courtCase.getCrn(), courtCase.getCourtCode());

        Optional.ofNullable(courtCase.getCrn())
            .map(crn -> courtCaseService.updateProbationStatusDetail(courtCase)
                .onErrorReturn(courtCase))
            .orElse(Mono.just(courtCase))
            .subscribe(courtCaseService::saveCourtCase);
    }

    @Override
    public TelemetryService getTelemetryService() {
        return telemetryService;
    }

    @Override
    public CourtCaseService getCourtCaseService() {
        return courtCaseService;
    }

}
