package uk.gov.justice.probation.courtcasematcher.service;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
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

    private static final int MAX_PROPERTY_COUNT = 13;
    static final String SOURCE_KEY = "source";
    static final String COURT_CODE_KEY = "courtCode";
    static final String COURT_ROOM_KEY = "courtRoom";
    static final String CASE_NO_KEY = "caseNo";
    static final String CASE_ID_KEY = "caseId";
    static final String MATCHED_BY_KEY = "matchedBy";
    static final String MATCHES_KEY = "matches";
    static final String PNC_KEY = "pnc";
    static final String CRNS_KEY = "crns";
    static final String HEARING_DATE_KEY = "hearingDate";
    static final String SQS_MESSAGE_ID_KEY = "sqsMessageId";
    static final String URN_KEY = "urn";
    static final String HEARING_ID_KEY = "hearingId";
    static final String DEFENDANT_IDS_KEY = "defendantIds";

    static final String DEFENDANT_ID_KEY = "defendantId";


    private final TelemetryClient telemetryClient;

    public void trackEvent(TelemetryEventType eventType) {
        telemetryClient.trackEvent(eventType.eventName);
    }

    public void trackOffenderMatchFailureEvent(Defendant defendant, CourtCase courtCase) {
        final var properties = getCourtCaseProperties(courtCase, defendant.getPnc());

        telemetryClient.trackEvent(TelemetryEventType.OFFENDER_MATCH_ERROR.eventName, properties, Collections.emptyMap());
    }

    public void trackOffenderMatchEvent(Defendant defendant, CourtCase courtCase, MatchResponse matchResponse) {
        if (matchResponse == null) {
            return;
        }

        final var properties = getCourtCaseProperties(courtCase, defendant.getPnc());

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
                properties.put(DEFENDANT_ID_KEY, defendant.getDefendantId());
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

        final var properties = getCourtCaseProperties(aCase);

        ofNullable(messageId)
          .ifPresent((code) -> properties.put(SQS_MESSAGE_ID_KEY, messageId));

        telemetryClient.trackEvent(TelemetryEventType.COURT_CASE_RECEIVED.eventName, properties, Collections.emptyMap());
    }

    public void trackHearingChangedEvent(CourtCase courtCase) {

        final var properties = getCourtCaseProperties(courtCase);

        telemetryClient.trackEvent(TelemetryEventType.HEARING_CHANGED.eventName, properties, Collections.emptyMap());
    }

    public void trackHearingUnChangedEvent(CourtCase courtCase) {

        final var properties = getCourtCaseProperties(courtCase);

        telemetryClient.trackEvent(TelemetryEventType.HEARING_UNCHANGED.eventName, properties, Collections.emptyMap());
    }

    private Map<String, String> getCourtCaseProperties(CourtCase courtCase) {
        Map<String, String> properties = new HashMap<>(MAX_PROPERTY_COUNT);
        ofNullable(courtCase.getCourtCode())
            .ifPresent((code) -> properties.put(COURT_CODE_KEY, code));
        ofNullable(courtCase.getCourtRoom())
            .ifPresent((courtRoom) -> properties.put(COURT_ROOM_KEY, courtRoom));
        ofNullable(courtCase.getCaseNo())
            .ifPresent((caseNo) -> properties.put(CASE_NO_KEY, caseNo));
        ofNullable(courtCase.getSessionStartTime())
          .map(date -> date.format(DateTimeFormatter.ISO_DATE))
          .ifPresent((date) -> properties.put(HEARING_DATE_KEY, date));
        ofNullable(courtCase.getCaseId())
          .ifPresent((caseId) -> properties.put(CASE_ID_KEY, caseId));
        ofNullable(courtCase.getSource())
          .ifPresent((source) -> properties.put(SOURCE_KEY, source.name()));
        ofNullable(courtCase.getUrn())
          .ifPresent((urn) -> properties.put(URN_KEY, urn));
        ofNullable(courtCase.getHearingId())
          .ifPresent(hearingId -> properties.put(HEARING_ID_KEY, hearingId));
        ofNullable(courtCase.getDefendants()).ifPresent(defendants -> {
            final var defendantIds = defendants.stream().map(Defendant::getDefendantId).collect(Collectors.joining(","));
            properties.put(DEFENDANT_IDS_KEY, defendantIds);
        });

        return properties;
    }

    private Map<String, String> getCourtCaseProperties(CourtCase courtCase, String pnc) {

        var properties = getCourtCaseProperties(courtCase);
        ofNullable(pnc)
                .ifPresent(p -> properties.put(PNC_KEY, p));

        return properties;
    }

    public void trackCaseMessageReceivedEvent(String messageID) {
        Map<String, String> properties = new HashMap<>(MAX_PROPERTY_COUNT);
        ofNullable(messageID)
            .ifPresent((code) -> properties.put(SQS_MESSAGE_ID_KEY, messageID));
        telemetryClient.trackEvent(TelemetryEventType.COURT_CASE_MESSAGE_RECEIVED.eventName, properties, Collections.emptyMap());
    }

    public AutoCloseable withOperation(String operationId) {
        telemetryClient.getContext().getOperation().setId(operationId);
        return () -> telemetryClient.getContext().getOperation().setId(null);
    }
}
