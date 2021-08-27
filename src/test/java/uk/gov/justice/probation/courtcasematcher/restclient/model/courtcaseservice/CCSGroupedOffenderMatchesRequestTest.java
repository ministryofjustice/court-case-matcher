package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.model.domain.MatchIdentifiers;
import uk.gov.justice.probation.courtcasematcher.model.domain.OffenderMatch;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchType;

import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CCSGroupedOffenderMatchesRequestTest {
    @Test
    public void map() {
        final GroupedOffenderMatches groupedOffenderMatches = GroupedOffenderMatches.builder()
                .matches(Collections.singletonList(
                        OffenderMatch.builder()
                                .confirmed(true)
                                .matchIdentifiers(MatchIdentifiers.builder()
                                        .crn("crn")
                                        .cro("cro")
                                        .pnc("pnc")
                                        .build())
                                .rejected(true)
                                .matchType(MatchType.NAME)
                                .build()
                ))
                .build();
        final var matchesRequest = CCSGroupedOffenderMatchesRequest.of(groupedOffenderMatches);

        assertThat(matchesRequest).usingRecursiveComparison().isEqualTo(groupedOffenderMatches);
    }

    @Test
    public void mapNull() {
        final GroupedOffenderMatches groupedOffenderMatches = GroupedOffenderMatches.builder()
                .matches(null)
                .build();
        final var matchesRequest = CCSGroupedOffenderMatchesRequest.of(groupedOffenderMatches);

        assertThat(matchesRequest.getMatches()).isEqualTo(Collections.emptyList());
    }

}
