package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Address;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CPAddress {
    @NotBlank
    private final String address1;
    private final String address2;
    private final String address3;
    private final String address4;
    private final String address5;
    private final String postcode;

    public uk.gov.justice.probation.courtcasematcher.model.domain.Address asDomain() {
        return Address.builder()
                .line1(address1)
                .line2(address2)
                .line3(address3)
                .line4(address4)
                .line5(address5)
                .postcode(postcode)
                .build();
    }
}
