package uk.gov.justice.probation.courtcasematcher.restclient.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressRequest {
    private final String line1;
    private final String line2;
    private final String line3;
    private final String line4;
    private final String line5;
    private final String postcode;

    public static AddressRequest of(uk.gov.justice.probation.courtcasematcher.model.domain.Address defendantAddress) {
        return AddressRequest.builder()
                .line1(defendantAddress.getLine1())
                .line2(defendantAddress.getLine2())
                .line3(defendantAddress.getLine3())
                .line4(defendantAddress.getLine4())
                .line5(defendantAddress.getLine5())
                .postcode(defendantAddress.getPostcode())
                .build();
    }
}
