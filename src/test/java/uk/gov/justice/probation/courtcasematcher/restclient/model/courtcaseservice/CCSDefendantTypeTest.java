package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType;

import static org.assertj.core.api.Assertions.assertThat;

class CCSDefendantTypeTest {
    @Test
    public void organisation() {
        assertThat(CCSDefendantType.of(DefendantType.ORGANISATION)).isEqualTo(CCSDefendantType.ORGANISATION);
    }

    @Test
    public void person() {
        assertThat(CCSDefendantType.of(DefendantType.PERSON)).isEqualTo(CCSDefendantType.PERSON);
    }

    @Test
    public void mapNull() {
        assertThat(CCSDefendantType.of(null)).isEqualTo(CCSDefendantType.PERSON);
    }
    
    @Test
    public void asDomain() {
        assertThat(CCSDefendantType.PERSON.asDomain()).isEqualTo(DefendantType.PERSON);
        assertThat(CCSDefendantType.ORGANISATION.asDomain()).isEqualTo(DefendantType.ORGANISATION);
    }
}
