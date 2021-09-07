package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CCSExtendedCase {
    private String caseId;
    private String caseNo;
    private String courtCode;
    private List<CCSDefendant> defendants;
    private List<CCSHearingDay> hearingDays;
    private CCSDataSource source;

    public static CCSExtendedCase of(CourtCase courtCase) {
        return CCSExtendedCase.builder()
                .caseId(courtCase.getCaseId())
                .caseNo(courtCase.getCaseNo())
                .courtCode(courtCase.getCourtCode())
                .source(CCSDataSource.of(courtCase.getSource()))
                .hearingDays(Collections.singletonList(CCSHearingDay.builder()
                        .courtCode(courtCase.getCourtCode())
                        .courtRoom(courtCase.getCourtRoom())
                        .sessionStartTime(courtCase.getSessionStartTime())
                        .listNo(courtCase.getListNo())
                        .build()))
                .defendants(Collections.singletonList(CCSDefendant.builder()
                        .defendantId(courtCase.getDefendantId())
                        .name(CCSName.of(courtCase.getName()))
                        .dateOfBirth(courtCase.getDefendantDob())
                        .address(CCSAddress.of(courtCase.getDefendantAddress()))
                        .type(CCSDefendantType.of(courtCase.getDefendantType()))
                        .probationStatus(courtCase.getProbationStatus())
                        .offences(courtCase.getOffences()
                                .stream()
                                .map(CCSOffence::of)
                                .collect(Collectors.toList()))
                        .crn(courtCase.getCrn())
                        .pnc(courtCase.getPnc())
                        .cro(courtCase.getCro())
                        .preSentenceActivity(courtCase.isPreSentenceActivity())
                        .suspendedSentenceOrder(courtCase.getSuspendedSentenceOrder())
                        .sex(courtCase.getDefendantSex())
                        .previouslyKnownTerminationDate(courtCase.getPreviouslyKnownTerminationDate())
                        .awaitingPsr(courtCase.isAwaitingPsr())
                        .breach(courtCase.getBreach())
                        .build()))
                .build();
    }
}
