package uk.gov.justice.probation.courtcasematcher.model.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchType;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class OffenderMatch {
    @NotNull
    @Valid
    private final MatchIdentifiers matchIdentifiers;
    @NotNull
    private final MatchType matchType;
    @NotNull
    private final Boolean confirmed;
    @NotNull
    private final Boolean rejected;
}
