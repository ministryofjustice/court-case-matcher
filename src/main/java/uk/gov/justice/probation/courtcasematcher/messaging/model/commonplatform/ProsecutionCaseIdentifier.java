package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class ProsecutionCaseIdentifier {
    @NotBlank
    @JsonProperty("caseURN")
    private final String caseUrn;
}
