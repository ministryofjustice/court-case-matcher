package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
        final var firstDefendant = courtCase.getFirstDefendant();
        return CCSExtendedCase.builder()
                .caseId(courtCase.getCaseId())
                .caseNo(courtCase.getCaseNo())
                .courtCode(courtCase.getCourtCode())
                .courtCode(courtCase.getCourtCode())
                .source(CCSDataSource.of(courtCase.getSource()))
                .hearingDays(courtCase.getHearingDays().stream()
                        .map((HearingDay courtCase1) -> CCSHearingDay.of(courtCase1))
                        .collect(Collectors.toList()))
                .defendants(Collections.singletonList(CCSDefendant.builder()
                        .defendantId(firstDefendant.getDefendantId())
                        .name(CCSName.of(firstDefendant.getName()))
                        .dateOfBirth(firstDefendant.getDateOfBirth())
                        .address(CCSAddress.of(firstDefendant.getAddress()))
                        .type(CCSDefendantType.of(firstDefendant.getType()))
                        .probationStatus(firstDefendant.getProbationStatus())
                        .offences(Optional.ofNullable(firstDefendant)
                                .map(Defendant::getOffences)
                                .orElse(Collections.emptyList())
                                .stream()
                                .map(CCSOffence::of)
                                .collect(Collectors.toList()))
                        .crn(firstDefendant.getCrn())
                        .pnc(firstDefendant.getPnc())
                        .cro(firstDefendant.getCro())
                        .preSentenceActivity(firstDefendant.getPreSentenceActivity())
                        .suspendedSentenceOrder(firstDefendant.getSuspendedSentenceOrder())
                        .sex(firstDefendant.getSex())
                        .previouslyKnownTerminationDate(firstDefendant.getPreviouslyKnownTerminationDate())
                        .awaitingPsr(firstDefendant.getAwaitingPsr())
                        .breach(firstDefendant.getBreach())
                        .build()))
                .build();
    }
}
