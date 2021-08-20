package uk.gov.justice.probation.courtcasematcher.restclient.model;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;

import static org.assertj.core.api.Assertions.assertThat;

class OffenceRequestTest {
    @Test
    public void mapOffenceRequest() {
        final var offence = Offence.builder()
                .offenceTitle("title")
                .offenceSummary("summary")
                .act("act")
                .sequenceNumber(1)
                .build();
        final var offenceRequest = OffenceRequest.of(offence);

        assertThat(offenceRequest).usingRecursiveComparison().isEqualTo(offence);

    }

}
