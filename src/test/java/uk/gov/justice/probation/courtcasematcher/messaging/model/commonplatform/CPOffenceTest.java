package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CPOffenceTest {
    @Test
    public void asDomain() {
        final var offence = CPOffence.builder()
                .id("id")
                .offenceTitle("title")
                .offenceLegislation("legislation")
                .wording("wording")
                .listNo(20)
                .build()
                .asDomain();

        assertThat(offence).isEqualTo(Offence.builder()
                        .id("id")
                        .offenceTitle("title")
                        .offenceSummary("wording")
                        .act("legislation")
                        .listNo(20)
                .build());
    }

}
