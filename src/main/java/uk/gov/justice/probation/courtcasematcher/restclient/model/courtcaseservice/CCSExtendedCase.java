package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import lombok.Builder;
import lombok.Data;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;

import java.util.Collections;
import java.util.List;

@Builder
@Data
public class CCSExtendedCase {
    private String caseId;
    private List<CCSHearingDay> hearingDays;
    private List<CCSDefendant> defendants;
    public static CCSExtendedCase of(CourtCase courtCase) {
        return CCSExtendedCase.builder()
                .caseId(courtCase.getCaseId())
                .hearingDays(Collections.singletonList(CCSHearingDay.builder()
                                .courtCode(courtCase.getCourtCode())
                                .courtRoom(courtCase.getCourtRoom())
                                .sessionStartTime(courtCase.getSessionStartTime())
                        .build()))
                .defendants(Collections.singletonList(CCSDefendant.builder()
                                .name(CCSName.of(courtCase.getName()))
                                .dateOfBirth(courtCase.getDefendantDob())
                                .address(CCSAddress.of(courtCase.getDefendantAddress()))
                                .type(CCSDefendantType.of(courtCase.getDefendantType()))
                                .probationStatus(courtCase.getProbationStatus())
                        .build()))
                .build();
    }
}
