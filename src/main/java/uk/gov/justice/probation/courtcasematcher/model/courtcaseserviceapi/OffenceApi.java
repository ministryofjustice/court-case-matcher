package uk.gov.justice.probation.courtcasematcher.model.courtcaseserviceapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OffenceApi {

    private String offenceTitle;

    private String offenceSummary;

    private String act;

    private Integer sequenceNumber;
}
