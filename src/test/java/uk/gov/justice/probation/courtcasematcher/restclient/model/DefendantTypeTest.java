package uk.gov.justice.probation.courtcasematcher.restclient.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefendantTypeTest {
    @Test
    public void organisation() {
        assertThat(DefendantType.of(uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType.ORGANISATION)).isEqualTo(DefendantType.ORGANISATION);
    }

    @Test
    public void person() {
        assertThat(DefendantType.of(uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType.PERSON)).isEqualTo(DefendantType.PERSON);
    }

    @Test
    public void mapNull() {
        assertThat(DefendantType.of(null)).isEqualTo(DefendantType.PERSON);
    }
}
