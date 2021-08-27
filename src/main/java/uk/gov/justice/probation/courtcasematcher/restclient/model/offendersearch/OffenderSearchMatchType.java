package uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch;

import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.probation.courtcasematcher.model.domain.MatchType;

import java.util.Optional;

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

    public static MatchType domainMatchTypeOf(SearchResult result) {
        var matchedBy = Optional.ofNullable(result.getMatchResponse())
                .map(MatchResponse::getMatchedBy)
                .orElse(null);

        return asDomain(matchedBy, result);
    }

    private static MatchType asDomain(OffenderSearchMatchType matchedBy, SearchResult result) {
        if (matchedBy == null)
            return MatchType.UNKNOWN;

        switch (matchedBy) {
            case ALL_SUPPLIED:
                final var pncInRequest = Optional.ofNullable(result.getMatchRequest())
                        .map(MatchRequest::getPncNumber)
                        .isPresent();
                if (pncInRequest) return MatchType.NAME_DOB_PNC;
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
                log.warn("Unknown OffenderSearchMatchType received {}", matchedBy.name());
                return MatchType.UNKNOWN;
        }
    }
}
