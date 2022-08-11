package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.JudicialResult;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
public class CPJudicialResult {
    private  boolean isConvictedResult;
    private String label;
    private String judicialResultTypeId;

    public JudicialResult asDomain(){
        return JudicialResult.builder()
                .isConvictedResult(isConvictedResult)
                .label(label)
                .judicialResultTypeId(judicialResultTypeId)
                .build();
    }


}
