package uk.gov.justice.probation.courtcasematcher.model.courtcaseservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefendantTypeTest {

    @DisplayName("Unknown defendant type")
    @Test
    void givenUnknownDefendantType_whenMatch_ThenReturnPerson() {
        assertThat(DefendantType.of("X")).isSameAs(DefendantType.PERSON);
    }

    @DisplayName("Organisation defendant type")
    @Test
    void givenOrganisationDefendantType_whenMatch_ThenReturn() {
        assertThat(DefendantType.of("O")).isSameAs(DefendantType.ORGANISATION);
        assertThat(DefendantType.of("o")).isSameAs(DefendantType.ORGANISATION);
    }

    @DisplayName("Person defendant type")
    @Test
    void givenPersonDefendantType_whenMatch_ThenReturnPerson() {
        assertThat(DefendantType.of("P")).isSameAs(DefendantType.PERSON);
        assertThat(DefendantType.of("p")).isSameAs(DefendantType.PERSON);
    }

    @DisplayName("null defendant type passed")
    @Test
    void givenPersonDefendantType_whenMatchOnNull_ThenReturnPerson() {
        assertThat(DefendantType.of(null)).isSameAs(DefendantType.PERSON);
    }
}
