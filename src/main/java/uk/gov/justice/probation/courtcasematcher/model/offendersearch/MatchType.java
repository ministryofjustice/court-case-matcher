package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

public enum MatchType {
    /**
     * This is how offender search describes the search, not used to store offender matches against.
     */
    ALL_SUPPLIED,
    NAME_DOB,
    HMPPS_KEY,
    EXTERNAL_KEY,
    NAME,
    PARTIAL_NAME,
    PARTIAL_NAME_DOB_LENIENT,
    NOTHING


}
