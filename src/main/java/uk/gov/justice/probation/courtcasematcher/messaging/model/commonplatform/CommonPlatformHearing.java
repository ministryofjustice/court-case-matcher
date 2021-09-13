package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor(access=AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
public class CommonPlatformHearing {
    @NotBlank
    private final String id;
    @NotNull
    @Valid
    private final CourtCentre courtCentre;
    @NotEmpty
    @Valid
    private final List<CPHearingDay> hearingDays;
    @NotNull
    private final JurisdictionType jurisdictionType;
    @NotEmpty
    @Valid
    private final List<ProsecutionCase> prosecutionCases;

    public CourtCase asDomain() {
        return CourtCase.builder()
                // TODO: This should be populated from prosecutionCaseId
                .caseId(id)
                .build();
    }
}
