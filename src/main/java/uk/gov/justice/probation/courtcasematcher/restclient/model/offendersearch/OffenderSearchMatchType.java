package uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch;

import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.probation.courtcasematcher.model.type.MatchType;

@Slf4j
public enum OffenderSearchMatchType {
    ALL_SUPPLIED,
    /** Matches to all the parameters supplied but at least one from the offender's alias */
    ALL_SUPPLIED_ALIAS,
    HMPPS_KEY,
    EXTERNAL_KEY,
    NAME,
    PARTIAL_NAME,
    PARTIAL_NAME_DOB_LENIENT,
    NOTHING;

    public MatchType asDomain(boolean pncPresent) {
        switch (this) {
            case ALL_SUPPLIED:
                if (pncPresent) return MatchType.NAME_DOB_PNC;
                else return MatchType.NAME_DOB;
            case ALL_SUPPLIED_ALIAS:
                return MatchType.NAME_DOB_ALIAS;
            case NAME:
                return MatchType.NAME;
            case PARTIAL_NAME:
                return MatchType.PARTIAL_NAME;
            case PARTIAL_NAME_DOB_LENIENT:
                return MatchType.PARTIAL_NAME_DOB_LENIENT;
            case EXTERNAL_KEY:
                return MatchType.EXTERNAL_KEY;
            case NOTHING:
                return MatchType.NOTHING;
            default:
                log.warn("Unknown OffenderSearchMatchType received {}", name());
                return MatchType.UNKNOWN;
        }
    }
}
