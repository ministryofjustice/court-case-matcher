package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CPOrganisationTest {
    @Test
    public void asName() {
        final var name = CPOrganisation.builder()
                .name("orgname")
                .build()
                .asName();

        assertThat(name).isEqualTo(Name.builder()
                        .surname("orgname")
                .build());
    }
}
