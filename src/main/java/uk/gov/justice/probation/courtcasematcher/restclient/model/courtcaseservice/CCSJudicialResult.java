package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.JudicialResult;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CCSJudicialResult {
    private boolean isConvictedResult;
    private String label;

    public static CCSJudicialResult of(JudicialResult judicialResult) {
        return CCSJudicialResult.builder()
                .isConvictedResult(judicialResult.isConvictedResult())
                .label(judicialResult.getLabel())
                .build();
    }

    public JudicialResult asDomain() {
        return JudicialResult.builder()
                .isConvictedResult(isConvictedResult())
                .label(getLabel())
                .build();

    }
}
