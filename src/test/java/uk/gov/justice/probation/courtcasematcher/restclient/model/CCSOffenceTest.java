package uk.gov.justice.probation.courtcasematcher.restclient.model;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.CCSOffence;

import static org.assertj.core.api.Assertions.assertThat;

class CCSOffenceTest {
    @Test
    public void mapOffenceRequest() {
        final var offence = buildOffence();
        final var ccsOffence = CCSOffence.of(offence);

        assertThat(ccsOffence).usingRecursiveComparison().isEqualTo(offence);

    }

    @Test
    public void mapBackToDomain() {
        final var original = buildOffence();
        final var actual = CCSOffence.of(original).asDomain();

        assertThat(actual).isEqualTo(original);
    }

    private Offence buildOffence() {
        return Offence.builder()
                .offenceTitle("title")
                .offenceSummary("summary")
                .act("act")
                .sequenceNumber(1)
                .build();
    }


}
