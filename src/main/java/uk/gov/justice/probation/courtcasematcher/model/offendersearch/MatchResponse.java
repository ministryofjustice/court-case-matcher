package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class MatchResponse {
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
