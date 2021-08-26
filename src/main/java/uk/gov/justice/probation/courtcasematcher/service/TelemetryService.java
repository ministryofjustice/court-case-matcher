package uk.gov.justice.probation.courtcasematcher.service;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchResponse;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Service
@AllArgsConstructor
public class TelemetryService {

    private static final int MAX_PROPERTY_COUNT = 8;
    static final String COURT_CODE_KEY = "courtCode";
    static final String COURT_ROOM_KEY = "courtRoom";
    static final String CASE_NO_KEY = "caseNo";
    static final String MATCHED_BY_KEY = "matchedBy";
    static final String MATCHES_KEY = "matches";
    static final String PNC_KEY = "pnc";
    static final String CRNS_KEY = "crns";
    static final String HEARING_DATE_KEY = "hearingDate";
    static final String SQS_MESSAGE_ID_KEY = "sqsMessageId";

    private final TelemetryClient telemetryClient;

    public void trackEvent(TelemetryEventType eventType) {
        telemetryClient.trackEvent(eventType.eventName);
    }

    public void trackOffenderMatchFailureEvent(CourtCase courtCase) {
        telemetryClient.trackEvent(TelemetryEventType.OFFENDER_MATCH_ERROR.eventName, getCourtCaseProperties(courtCase), Collections.emptyMap());
    }

    public void trackOffenderMatchEvent(CourtCase courtCase, MatchResponse matchResponse) {
        if (matchResponse == null) {
            return;
        }

        Map<String, String> properties = getCourtCaseProperties(courtCase);

        int matchCount = matchResponse.getMatchCount();
        ofNullable(matchResponse.getMatchedBy())
                .filter((matchedBy) -> matchCount >= 1)
                .ifPresent((matchedBy) -> properties.put(MATCHED_BY_KEY, matchedBy.name()));
        ofNullable(matchResponse.getMatches())
            .ifPresent((matches -> {
                String allCrns = matches.stream()
                    .map(match -> match.getOffender().getOtherIds().getCrn())
                    .collect(Collectors.joining(","));
                properties.put(MATCHES_KEY, String.valueOf(matches.size()));
                properties.put(CRNS_KEY, allCrns);
            }));

        TelemetryEventType eventType = TelemetryEventType.OFFENDER_PARTIAL_MATCH;
        if (matchResponse.isExactMatch()) {
            eventType = TelemetryEventType.OFFENDER_EXACT_MATCH;
        }
        else if (matchCount == 0){
            eventType = TelemetryEventType.OFFENDER_NO_MATCH;
        }
        telemetryClient.trackEvent(eventType.eventName, properties, Collections.emptyMap());
    }

    public void trackCourtCaseEvent(CourtCase aCase, String messageId) {

        Map<String, String> properties = new HashMap<>(5);
        ofNullable(aCase.getCourtCode())
            .ifPresent((code) -> properties.put(COURT_CODE_KEY, code));
        ofNullable(aCase.getCourtRoom())
            .ifPresent((courtRoom) -> properties.put(COURT_ROOM_KEY, courtRoom));
        ofNullable(aCase.getDateOfHearing())
            .ifPresent((dateOfHearing) -> properties.put(HEARING_DATE_KEY, dateOfHearing.toString()));
        ofNullable(aCase.getCaseNo())
            .ifPresent((caseNo) -> properties.put(CASE_NO_KEY, caseNo));
        ofNullable(messageId)
                .ifPresent((code) -> properties.put(SQS_MESSAGE_ID_KEY, messageId));

        telemetryClient.trackEvent(TelemetryEventType.COURT_CASE_RECEIVED.eventName, properties, Collections.emptyMap());
    }

    private Map<String, String> getCourtCaseProperties(CourtCase courtCase) {
        Map<String, String> properties = new HashMap<>(MAX_PROPERTY_COUNT);
        ofNullable(courtCase.getCourtCode())
            .ifPresent((code) -> properties.put(COURT_CODE_KEY, code));
        ofNullable(courtCase.getCaseNo())
            .ifPresent((caseNo) -> properties.put(CASE_NO_KEY, caseNo));
        ofNullable(courtCase.getSessionStartTime())
            .map(date -> date.format(DateTimeFormatter.ISO_DATE))
            .ifPresent((date) -> properties.put(HEARING_DATE_KEY, date));
        ofNullable(courtCase.getPnc())
            .ifPresent((pnc) -> properties.put(PNC_KEY, pnc));
        return properties;
    }

    public void trackCaseMessageReceivedEvent(String messageID) {
        Map<String, String> properties = new HashMap<>(MAX_PROPERTY_COUNT);
        ofNullable(messageID)
            .ifPresent((code) -> properties.put(SQS_MESSAGE_ID_KEY, messageID));
        telemetryClient.trackEvent(TelemetryEventType.COURT_CASE_MESSAGE_RECEIVED.eventName, properties, Collections.emptyMap());
    }

    public void trackCourtListMessageEvent(String messageID) {
        Map<String, String> properties = new HashMap<>(MAX_PROPERTY_COUNT);
        ofNullable(messageID)
            .ifPresent((code) -> properties.put(SQS_MESSAGE_ID_KEY, messageID));
        telemetryClient.trackEvent(TelemetryEventType.COURT_LIST_MESSAGE_RECEIVED.eventName, properties, Collections.emptyMap());
    }

    public AutoCloseable withOperation(String operationId) {
        telemetryClient.getContext().getOperation().setId(operationId);
        return () -> telemetryClient.getContext().getOperation().setId(null);
    }
}
