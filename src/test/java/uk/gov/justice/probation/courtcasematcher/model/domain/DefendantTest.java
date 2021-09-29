package uk.gov.justice.probation.courtcasematcher.model.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class DefendantTest {

    @Test
    void givenOrg_whenShouldMatch_thenReturnFalse() {
        var courtCase = Defendant.builder()
                .type(DefendantType.ORGANISATION)
                .build();
        assertThat(courtCase.shouldMatchToOffender()).isFalse();
    }

    @Test
    void givenMatchedPerson_whenShouldMatch_thenReturnFalse() {
        var courtCase = Defendant.builder()
                .type(DefendantType.PERSON)
                .crn("X321567")
                .build();
        assertThat(courtCase.shouldMatchToOffender()).isFalse();
    }

    @Test
    void givenSpacesOnlyInCrnPerson_whenShouldMatch_thenReturnTrue() {
        var courtCase = Defendant.builder()
                        .type(DefendantType.PERSON)
                        .crn("   ")
                        .build();
        assertThat(courtCase.shouldMatchToOffender()).isTrue();
    }

    @Test
    void givenUnMatchedPerson_whenShouldMatch_thenReturnTrue() {
        var courtCase = Defendant.builder()
                        .type(DefendantType.PERSON)
                        .build();
        assertThat(courtCase.shouldMatchToOffender()).isTrue();
    }
}
