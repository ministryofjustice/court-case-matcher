package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.JudicialResult;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CPOffenceTest {
    @Test
    public void asDomain() {
        final var offence = CPOffence.builder()
                .id("id")
                .offenceTitle("title")
                .offenceLegislation("legislation")
                .wording("wording")
                .listingNumber(20)
                .judicialResults(List.of(CPJudicialResult.builder()
                        .build()))
                .build()
                .asDomain();

        assertThat(offence).isEqualTo(Offence.builder()
                .id("id")
                .offenceTitle("title")
                .offenceSummary("wording")
                .act("legislation")
                .listNo(20)
                .judicialResults(List.of(JudicialResult.builder()
                        .build()))
                .build());
    }

}
