package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.MatchType;
import uk.gov.justice.probation.courtcasematcher.model.domain.OffenderMatch;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CCSOffenderMatch {
    @NotNull
    @Valid
    private final CCSMatchIdentifiers matchIdentifiers;
    @NotNull
    private final MatchType matchType;
    @NotNull
    private final Boolean confirmed;
    @NotNull
    private final Boolean rejected;

    public static CCSOffenderMatch of(OffenderMatch offenderMatch) {
        return CCSOffenderMatch.builder()
                .matchType(offenderMatch.getMatchType())
                .confirmed(offenderMatch.getConfirmed())
                .matchIdentifiers(CCSMatchIdentifiers.of(offenderMatch.getMatchIdentifiers()))
                .rejected(offenderMatch.getRejected())
                .build();
    }
}