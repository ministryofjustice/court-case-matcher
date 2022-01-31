package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

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
public class CCSOffence {

    private final String offenceTitle;

    private final String offenceSummary;

    private final String act;

    private final Integer sequenceNumber;

    private final Integer listNo;

    public static CCSOffence of(Offence offence) {
        return CCSOffence.builder()
                .offenceTitle(offence.getOffenceTitle())
                .offenceSummary(offence.getOffenceSummary())
                .act(offence.getAct())
                .sequenceNumber(offence.getSequenceNumber())
                .listNo(offence.getListNo())
                .build();
    }

    public Offence asDomain() {
        return Offence.builder()
                .offenceTitle(getOffenceTitle())
                .offenceSummary(getOffenceSummary())
                .act(getAct())
                .sequenceNumber(getSequenceNumber())
                .listNo(listNo)
                .build();
    }
}
