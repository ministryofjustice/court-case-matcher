package uk.gov.justice.probation.courtcasematcher.model.mapper;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import uk.gov.justice.probation.courtcasematcher.model.domain.MatchType;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.Match;

import java.util.Collections;
import java.util.List;

@Builder
@Data
public class MatchDetails {
    private final MatchType matchType;
    @Getter(AccessLevel.NONE)
    private final List<Match> matches;
    private final boolean exactMatch;

    public List<Match> getMatches() {
        return matches == null ? Collections.emptyList() : matches;
    }
}
