package uk.gov.justice.probation.courtcasematcher.model.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CourtCaseTest {

    @Test
    void givenAnyDefendantShouldMatch_thenTrue() {
        CourtCase courtCase = CourtCase.builder()
                .defendants(List.of(Defendant.builder()
                        .type(DefendantType.ORGANISATION)
                        .build(),
                        // This one should be matched
                        Defendant.builder()
                                .type(DefendantType.PERSON)
                                .build()
                ))
                .build();
        assertThat(courtCase.shouldMatchToOffender()).isTrue();
    }

    @Test
    void givenNoDefendantsShouldMatch_thenFalse() {
        CourtCase courtCase = CourtCase.builder()
                .defendants(List.of(Defendant.builder()
                        .type(DefendantType.ORGANISATION)
                        .build(),
                        Defendant.builder()
                                .type(DefendantType.PERSON)
                                .crn("something")
                                .build()
                ))
                .build();
        assertThat(courtCase.shouldMatchToOffender()).isFalse();
    }
}
