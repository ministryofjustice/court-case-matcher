package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CCSGroupedOffenderMatchesRequest {
    @NotNull
    @JsonProperty("matches")
    @Valid
    private final List<CCSOffenderMatch> matches;

    public static CCSGroupedOffenderMatchesRequest of(GroupedOffenderMatches matches) {
        return CCSGroupedOffenderMatchesRequest.builder()
                .matches(Optional.ofNullable(matches.getMatches())
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(CCSOffenderMatch::of)
                        .collect(Collectors.toList())
                )
                .build();
    }
}
