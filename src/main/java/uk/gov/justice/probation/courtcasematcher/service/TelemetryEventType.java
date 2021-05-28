package uk.gov.justice.probation.courtcasematcher.service;

public enum TelemetryEventType {
    OFFENDER_EXACT_MATCH("PiCOffenderExactMatch"),
    OFFENDER_PARTIAL_MATCH("PiCOffenderPartialMatch"),
    OFFENDER_NO_MATCH("PiCOffenderNoMatch"),
    OFFENDER_MATCH_ERROR("PiCOffenderMatchError"),
    COURT_CASE_MESSAGE_RECEIVED("PiCCourtCaseMessageReceived"),
    COURT_LIST_RECEIVED("PiCCourtListReceived"),
    COURT_LIST_MESSAGE_RECEIVED("PiCCourtListMessageReceived"),
    COURT_CASE_RECEIVED("PiCCourtCaseReceived")
    ;

    final String eventName;

    TelemetryEventType(String eventName) {
        this.eventName = eventName;
    }
}
