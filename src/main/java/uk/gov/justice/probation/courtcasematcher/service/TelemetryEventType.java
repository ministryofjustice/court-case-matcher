package uk.gov.justice.probation.courtcasematcher.service;

public enum TelemetryEventType {
    OFFENDER_EXACT_MATCH("PiCOffenderExactMatch"),
    OFFENDER_PARTIAL_MATCH("PiCOffenderPartialMatch"),
    OFFENDER_NO_MATCH("PiCOffenderNoMatch"),
    OFFENDER_MATCH_ERROR("PiCOffenderMatchError"),
    HEARING_MESSAGE_RECEIVED("PiCHearingMessageReceived"),
    COURT_LIST_RECEIVED("PiCCourtListReceived"),
    COURT_LIST_MESSAGE_RECEIVED("PiCCourtListMessageReceived"),
    HEARING_RECEIVED("PiCHearingReceived"),
    HEARING_CHANGED("PiCHearingChanged"),
    HEARING_UNCHANGED("PiCHearingUnchanged"),
    PROCESSING_FAILURE("PiCMatcherProcessingFailure");

    final String eventName;

    TelemetryEventType(String eventName) {
        this.eventName = eventName;
    }
}
