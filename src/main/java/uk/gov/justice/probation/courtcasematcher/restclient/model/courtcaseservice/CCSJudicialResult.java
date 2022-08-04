package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.JudicialResult;
import uk.gov.justice.probation.courtcasematcher.model.domain.JudicialResultType;

import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CCSJudicialResult {
    private boolean isConvictedResult;
    private String label;
    private CCSJudicialResultType judicialResultType;

    public static CCSJudicialResult of(JudicialResult judicialResult) {
        return CCSJudicialResult.builder()
                .isConvictedResult(judicialResult.isConvictedResult())
                .label(judicialResult.getLabel())
                .judicialResultType(CCSJudicialResultType.builder()
                        .description(judicialResult.getJudicialResultType().getDescription())
                        .id(judicialResult.getJudicialResultType().getId())
                        .build())
                .build();
    }

    public JudicialResult asDomain() {
        return JudicialResult.builder()
                .isConvictedResult(isConvictedResult())
                .label(getLabel())
                .judicialResultType(JudicialResultType.builder()
                        .description(judicialResultType.getDescription())
                        .id(judicialResultType.getId())
                        .build())
                .build();

    }
}
