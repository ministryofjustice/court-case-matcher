package uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.util.List;
import java.util.Optional;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class MatchResponse {
    @With
    private final List<Match> matches;
    private final OffenderSearchMatchType matchedBy;

    @JsonIgnore
    public boolean isExactMatch() {
        return getMatchCount() == 1 && matchedBy == OffenderSearchMatchType.ALL_SUPPLIED;
    }

    @JsonIgnore
    public int getMatchCount() {
        return Optional.ofNullable(matches).map(List::size).orElse(0);
    }
}
