package uk.gov.justice.probation.courtcasematcher.model.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class GroupedOffenderMatches {
    @NotNull
    @JsonProperty("matches")
    @Valid
    private final List<OffenderMatch> matches;
}
