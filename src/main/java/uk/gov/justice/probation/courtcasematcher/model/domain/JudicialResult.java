package uk.gov.justice.probation.courtcasematcher.model.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class JudicialResult {
    private boolean isConvictedResult;
    private String label;
    private String judicialResultTypeId;
    private String resultText;
}
