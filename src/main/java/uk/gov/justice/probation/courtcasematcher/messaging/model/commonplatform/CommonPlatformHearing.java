package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;

import java.util.List;

@Data
@Builder
@NoArgsConstructor(access=AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
public class CommonPlatformHearing {
    private final String id;
    private final CourtCentre courtCentre;
    private final List<HearingDay> hearingDays;
    private final JurisdictionType jurisdictionType;
    private final List<ProsecutionCase> prosecutionCases;

    public CourtCase asDomain() {
        return CourtCase.builder()
                // TODO: This should be populated from prosecutionCaseId
                .caseId(id)
                .build();
    }
}
