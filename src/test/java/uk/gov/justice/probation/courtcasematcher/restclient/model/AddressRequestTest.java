package uk.gov.justice.probation.courtcasematcher.restclient.model;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.Address;

import static org.assertj.core.api.Assertions.assertThat;

class AddressRequestTest {
    @Test
    public void whenOfAddress_thenMapAddress() {
        final var address = Address.builder()
                .line1("line1")
                .line2("line2")
                .line3("line3")
                .line4("line4")
                .line5("line5")
                .postcode("postypostpost")
                .build();

        final var addressRequest = AddressRequest.of(address);

        assertThat(addressRequest).usingRecursiveComparison().isEqualTo(address);
    }

}
