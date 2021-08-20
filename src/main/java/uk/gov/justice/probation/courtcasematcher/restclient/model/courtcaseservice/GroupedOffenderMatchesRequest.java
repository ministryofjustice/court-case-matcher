package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class GroupedOffenderMatchesRequest {
    @NotNull
    @JsonProperty("matches")
    @Valid
    private final List<OffenderMatch> matches;

    public static GroupedOffenderMatchesRequest of(GroupedOffenderMatches matches) {
        return GroupedOffenderMatchesRequest.builder().build();
    }
}
