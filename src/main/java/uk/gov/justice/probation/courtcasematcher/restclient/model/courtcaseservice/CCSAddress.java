package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Address;

@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CCSAddress {
    private final String line1;
    private final String line2;
    private final String line3;
    private final String line4;
    private final String line5;
    private final String postcode;

    public static CCSAddress of(uk.gov.justice.probation.courtcasematcher.model.domain.Address defendantAddress) {
        return CCSAddress.builder()
                .line1(defendantAddress.getLine1())
                .line2(defendantAddress.getLine2())
                .line3(defendantAddress.getLine3())
                .line4(defendantAddress.getLine4())
                .line5(defendantAddress.getLine5())
                .postcode(defendantAddress.getPostcode())
                .build();
    }

    public Address asDomain() {
        return Address.builder()
                .line1(getLine1())
                .line2(getLine2())
                .line3(getLine3())
                .line4(getLine4())
                .line5(getLine5())
                .postcode(getPostcode())
                .build();
    }
}
