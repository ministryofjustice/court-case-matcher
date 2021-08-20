package uk.gov.justice.probation.courtcasematcher.restclient.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OffenceRequest {

    private final String offenceTitle;

    private final String offenceSummary;

    private final String act;

    private final Integer sequenceNumber;

    public static OffenceRequest of(Offence offence) {
        return OffenceRequest.builder()
                .offenceTitle(offence.getOffenceTitle())
                .offenceSummary(offence.getOffenceSummary())
                .act(offence.getAct())
                .sequenceNumber(offence.getSequenceNumber())
                .build();
    }
}
