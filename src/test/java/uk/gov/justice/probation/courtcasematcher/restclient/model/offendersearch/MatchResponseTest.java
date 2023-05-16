package uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("MatchResponse is component to receive")
class MatchResponseTest {

    private final Match match = Match.builder().offender(OSOffender.builder().build()).build();

    @DisplayName("Search with one match for ALL_SUPPLIED is exact")
    @Test
    void givenSingleMatchAllSupplied_whenIsExact_thenReturnTrue() {
        MatchResponse matchResponse = MatchResponse.builder()
            .matches(List.of(match))
            .matchedBy(OffenderSearchMatchType.ALL_SUPPLIED)
            .build();

        assertThat(matchResponse.isExactOffenderMatch()).isTrue();
    }

    @DisplayName("Search with one match for anything but ALL_SUPPLIED is NOT exact")
    @Test
    void givenSingleMatchPartialOrName_whenIsExact_thenReturnFalse() {
        MatchResponse matchResponse = MatchResponse.builder()
            .matches(List.of(match))
            .matchedBy(OffenderSearchMatchType.PARTIAL_NAME)
            .build();

        assertThat(matchResponse.isExactOffenderMatch()).isFalse();
    }

    @DisplayName("Search with multiple matches is always NOT exact")
    @Test
    void givenMultipleMatches_whenIsExact_thenAlwaysReturnFalse() {
        Match match2 = Match.builder().offender(OSOffender.builder().build()).build();

        MatchResponse matchResponse = MatchResponse.builder()
            .matches(List.of(match, match2))
            .matchedBy(OffenderSearchMatchType.ALL_SUPPLIED)
            .build();

        assertThat(matchResponse.isExactOffenderMatch()).isFalse();
    }

    @DisplayName("Search with no matches is always NOT exact")
    @Test
    void givenZeroMatches_whenIsExact_thenAlwaysReturnFalse() {
        MatchResponse matchResponse = MatchResponse.builder()
            .matchedBy(OffenderSearchMatchType.NOTHING)
            .build();

        assertThat(matchResponse.isExactOffenderMatch()).isFalse();

        matchResponse = MatchResponse.builder()
            .matchedBy(OffenderSearchMatchType.ALL_SUPPLIED)
            .build();

        assertThat(matchResponse.isExactOffenderMatch()).isFalse();
    }
}
