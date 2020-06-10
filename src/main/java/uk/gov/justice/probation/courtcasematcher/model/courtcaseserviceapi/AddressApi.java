package uk.gov.justice.probation.courtcasematcher.model.courtcaseserviceapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressApi {
    private String line1;
    private String line2;
    private String line3;
    private String line4;
    private String line5;
    private String postcode;
}
