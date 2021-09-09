package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;

@Data
@Builder
public class CommonPlatformHearing {
    @NonNull
    private final String caseId;

    public CourtCase asDomain() {
        return CourtCase.builder()
                .caseId(caseId)
                .build();
    }
}
