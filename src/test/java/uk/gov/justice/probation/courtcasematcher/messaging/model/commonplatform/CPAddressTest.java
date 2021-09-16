package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.Address;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CPAddressTest {
    @Test
    public void asDomain() {
        final var address = CPAddress.builder()
                .address1("address1")
                .address2("address2")
                .address3("address3")
                .address4("address4")
                .address5("address5")
                .postcode("postcode")
                .build()
                .asDomain();

        assertThat(address).isEqualTo(Address.builder()
                .line1("address1")
                .line2("address2")
                .line3("address3")
                .line4("address4")
                .line5("address5")
                .postcode("postcode")
                .build());
    }

}
