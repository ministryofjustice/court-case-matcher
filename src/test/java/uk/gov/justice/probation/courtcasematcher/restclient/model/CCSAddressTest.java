package uk.gov.justice.probation.courtcasematcher.restclient.model;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.Address;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.CCSAddress;

import static org.assertj.core.api.Assertions.assertThat;

class CCSAddressTest {
    @Test
    public void map() {
        final var address = buildAddress();

        final var addressRequest = CCSAddress.of(address);

        assertThat(addressRequest).usingRecursiveComparison().isEqualTo(address);
    }

    @Test
    public void mapBack() {
        final var original = buildAddress();

        final var actual = CCSAddress.of(original).asDomain();

        assertThat(actual).isEqualTo(original);
    }

    private Address buildAddress() {
        return Address.builder()
                .line1("line1")
                .line2("line2")
                .line3("line3")
                .line4("line4")
                .line5("line5")
                .postcode("postypostpost")
                .build();
    }

}
