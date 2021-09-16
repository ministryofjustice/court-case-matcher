package uk.gov.justice.probation.courtcasematcher.model.domain;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CourtCaseTest {

    @Test
    void givenOrg_whenShouldMatch_thenReturnFalse() {
        CourtCase courtCase = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .type(DefendantType.ORGANISATION)
                        .build()))
                .build();
        assertThat(courtCase.shouldMatchToOffender()).isFalse();
    }

    @Test
    void givenMatchedPerson_whenShouldMatch_thenReturnFalse() {
        CourtCase courtCase = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .type(DefendantType.PERSON)
                        .crn("X321567")
                        .build()))
                .build();
        assertThat(courtCase.shouldMatchToOffender()).isFalse();
    }

    @Test
    void givenSpacesOnlyInCrnPerson_whenShouldMatch_thenReturnTrue() {
        CourtCase courtCase = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .type(DefendantType.PERSON)
                        .crn("   ")
                        .build()))
                .build();
        assertThat(courtCase.shouldMatchToOffender()).isTrue();
    }

    @Test
    void givenUnMatchedPerson_whenShouldMatch_thenReturnTrue() {
        CourtCase courtCase = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .type(DefendantType.PERSON)
                        .build()))
                .build();
        assertThat(courtCase.shouldMatchToOffender()).isTrue();
    }
}
