package uk.gov.justice.probation.courtcasematcher.service;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchResponse;

import java.time.format.DateTimeFormatter;
import java.util.*;
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
    static final String PERSON_ID_KEY = "personId";
    static final String DEFENDANT_IDS_KEY = "defendantIds";
    static final String AWAITING_PSR_KEY = "awaitingPsr";
    static final String IN_BREACH_KEY = "inBreach";
    static final String PRE_SENTENCE_ACTVITY_KEY = "preSentenceActivity";
    static final String PREVIOUSLY_KNOWN_TERMINATION_DATE_KEY = "previouslyKnownTerminationData";
    static final String PROBATION_STATUS_KEY = "status";
    static final String CRN_KEY = "crn";

    static final String DEFENDANT_ID_KEY = "defendantId";


    private final TelemetryClient telemetryClient;

    public void track404HearingProcessedEvent(Map<String, String> properties) {
        telemetryClient.trackEvent(TelemetryEventType.MISSING_HEARING_EVENT_PROCESSED.eventName, properties, Collections.emptyMap());
    }

    public void trackEvent(TelemetryEventType eventType) {
        telemetryClient.trackEvent(eventType.eventName);
    }

    public void trackOffenderMatchFailureEvent(Defendant defendant, Hearing hearing) {
        var properties = getHearingProperties(hearing, defendant.getPnc());
        properties.put(DEFENDANT_ID_KEY, defendant.getDefendantId());

        telemetryClient.trackEvent(TelemetryEventType.OFFENDER_MATCH_ERROR.eventName, properties, Collections.emptyMap());
    }

    public void trackOffenderMatchEvent(Defendant defendant, Hearing hearing, MatchResponse matchResponse) {
        if (matchResponse == null) {
            return;
        }

        final var properties = getHearingProperties(hearing, defendant.getPnc());

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
        if (matchResponse.isExactOffenderMatch()) {
            ofNullable(defendant.getPersonId())
                    .ifPresent(personId -> properties.put(PERSON_ID_KEY, personId));

            eventType = TelemetryEventType.OFFENDER_EXACT_MATCH;
        }
        else if (matchCount == 0){
            eventType = TelemetryEventType.OFFENDER_NO_MATCH;
        }
        telemetryClient.trackEvent(eventType.eventName, properties, Collections.emptyMap());
    }

    public void trackNewHearingEvent(Hearing hearing, String messageId) {

        final var properties = getHearingProperties(hearing);

        ofNullable(messageId)
          .ifPresent((code) -> properties.put(SQS_MESSAGE_ID_KEY, messageId));

        telemetryClient.trackEvent(TelemetryEventType.HEARING_RECEIVED.eventName, properties, Collections.emptyMap());
    }

    public void trackHearingChangedEvent(Hearing hearing) {

        final var properties = getHearingProperties(hearing);

        telemetryClient.trackEvent(TelemetryEventType.HEARING_CHANGED.eventName, properties, Collections.emptyMap());
    }

    public void trackHearingUnChangedEvent(Hearing hearing) {

        final var properties = getHearingProperties(hearing);

        telemetryClient.trackEvent(TelemetryEventType.HEARING_UNCHANGED.eventName, properties, Collections.emptyMap());
    }

    public void trackHearingMessageReceivedEvent(String messageID) {
        Map<String, String> properties = new HashMap<>(MAX_PROPERTY_COUNT);
        ofNullable(messageID)
            .ifPresent((code) -> properties.put(SQS_MESSAGE_ID_KEY, messageID));
        telemetryClient.trackEvent(TelemetryEventType.HEARING_MESSAGE_RECEIVED.eventName, properties, Collections.emptyMap());
    }

    public AutoCloseable withOperation(String operationId) {
        telemetryClient.getContext().getOperation().setId(operationId);
        return () -> telemetryClient.getContext().getOperation().setId(null);
    }

    public void trackProcessingFailureEvent(Hearing hearing) {
        final var properties = getHearingProperties(hearing);
        telemetryClient.trackEvent(TelemetryEventType.PROCESSING_FAILURE.eventName, properties, Collections.emptyMap());
    }

    public void trackDefendantProbationStatusUpdatedEvent(Defendant defendant) {
        telemetryClient.trackEvent(TelemetryEventType.PROBATION_STATUS_UPDATED.eventName, getDefendantProperties(defendant), Collections.emptyMap());

    }

    public void trackDefendantProbationStatusNotUpdatedEvent(Defendant defendant) {
        telemetryClient.trackEvent(TelemetryEventType.PROBATION_STATUS_NOT_UPDATED.eventName, getDefendantProperties(defendant), Collections.emptyMap());

    }

    private Map<String, String> getHearingProperties(Hearing hearing) {
        Map<String, String> properties = new HashMap<>(MAX_PROPERTY_COUNT);
        ofNullable(hearing.getCourtCode())
                .ifPresent((code) -> properties.put(COURT_CODE_KEY, code));
        ofNullable(hearing.getCourtRoom())
                .ifPresent((courtRoom) -> properties.put(COURT_ROOM_KEY, courtRoom));
        ofNullable(hearing.getCaseNo())
                .ifPresent((caseNo) -> properties.put(CASE_NO_KEY, caseNo));
        ofNullable(hearing.getSessionStartTime())
                .map(date -> date.format(DateTimeFormatter.ISO_DATE))
                .ifPresent((date) -> properties.put(HEARING_DATE_KEY, date));
        ofNullable(hearing.getCaseId())
                .ifPresent((caseId) -> properties.put(CASE_ID_KEY, caseId));
        ofNullable(hearing.getSource())
                .ifPresent((source) -> properties.put(SOURCE_KEY, source.name()));
        ofNullable(hearing.getUrn())
                .ifPresent((urn) -> properties.put(URN_KEY, urn));
        ofNullable(hearing.getHearingId())
                .ifPresent(hearingId -> properties.put(HEARING_ID_KEY, hearingId));
        ofNullable(hearing.getDefendants()).ifPresent(defendants -> {
            final var defendantIds = defendants.stream().map(Defendant::getDefendantId).collect(Collectors.joining(","));
            properties.put(DEFENDANT_IDS_KEY, defendantIds);
        });

        return properties;
    }

    private Map<String, String> getHearingProperties(Hearing hearing, String pnc) {

        var properties = getHearingProperties(hearing);
        ofNullable(pnc)
                .ifPresent(p -> properties.put(PNC_KEY, p));

        return properties;
    }

    private Map<String, String> getDefendantProperties(Defendant defendant) {
        Map<String, String> properties = new HashMap<>(MAX_PROPERTY_COUNT);
        Optional.ofNullable(defendant).ifPresent(defendant1 -> {
            ofNullable(defendant.getDefendantId())
              .ifPresent(defendantId -> properties.put(DEFENDANT_ID_KEY, defendantId));
            ofNullable(defendant.getCrn())
              .ifPresent(crn -> properties.put(CRN_KEY, crn));
            ofNullable(defendant.getAwaitingPsr())
              .ifPresent(awaitingPsr -> properties.put(AWAITING_PSR_KEY, awaitingPsr.toString()));
            ofNullable(defendant.getBreach())
              .ifPresent(breach -> properties.put(IN_BREACH_KEY, breach.toString()));
            ofNullable(defendant.getPreSentenceActivity())
              .ifPresent(preSentenceActivity -> properties.put(PRE_SENTENCE_ACTVITY_KEY, preSentenceActivity.toString()));
            ofNullable(defendant.getPreviouslyKnownTerminationDate())
              .ifPresent(previouslyKnownTerminationDate -> properties.put(PREVIOUSLY_KNOWN_TERMINATION_DATE_KEY, previouslyKnownTerminationDate.toString()));
            ofNullable(defendant.getProbationStatus())
              .ifPresent(probationStatus -> properties.put(PROBATION_STATUS_KEY, probationStatus));
        });

        return properties;
    }

    public void trackPersonRecordCreatedEvent(Defendant defendant, Hearing hearing) {
        Map<String, String> properties = new HashMap<>(MAX_PROPERTY_COUNT);

        ofNullable(hearing.getHearingId())
                .ifPresent(hearingId -> properties.put(HEARING_ID_KEY, hearingId));
        ofNullable(defendant.getPersonId())
                .ifPresent(personId -> properties.put(PERSON_ID_KEY, personId));
        ofNullable(defendant.getDefendantId())
                .ifPresent(defendantId -> properties.put(DEFENDANT_ID_KEY, defendantId));
        ofNullable(hearing.getCaseId())
                .ifPresent(caseId -> properties.put(CASE_ID_KEY, caseId));
        ofNullable(defendant.getPnc())
                .ifPresent(pnc -> properties.put(PNC_KEY, pnc));

        telemetryClient.trackEvent(TelemetryEventType.PERSON_RECORD_CREATED.eventName, properties, Collections.emptyMap());
    }


}
