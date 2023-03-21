package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CPProsecutionCase {
    @NotBlank
    private final String id;
    @NotNull
    @Valid
    private final List<CPDefendant> defendants;

    @NotNull
    @Valid
    private final ProsecutionCaseIdentifier prosecutionCaseIdentifier;

    @Valid
    private final List<CPCaseMarker> caseMarkers;
}
