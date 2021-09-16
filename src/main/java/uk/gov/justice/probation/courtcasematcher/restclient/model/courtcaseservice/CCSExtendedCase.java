package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
                .caseId(Optional.ofNullable(courtCase.getCaseId())
                        .orElseGet(CCSExtendedCase::generateUUID))
                .caseNo(courtCase.getCaseNo())
                .courtCode(courtCase.getCourtCode())
                .courtCode(courtCase.getCourtCode())
                .source(CCSDataSource.of(courtCase.getSource()))
                .hearingDays(courtCase.getHearingDays().stream()
                        .map(CCSHearingDay::of)
                        .collect(Collectors.toList()))
                .defendants(courtCase.getDefendants().stream()
                        .map(CCSExtendedCase::getDefendant)
                        .collect(Collectors.toList()))
                .build();
    }

    public static CCSDefendant getDefendant(Defendant defendant) {
        return CCSDefendant.builder()
                .defendantId(Optional.ofNullable(defendant.getDefendantId())
                        .orElseGet(CCSExtendedCase::generateUUID))
                .name(CCSName.of(defendant.getName()))
                .dateOfBirth(defendant.getDateOfBirth())
                .address(Optional.ofNullable(defendant.getAddress()).map(CCSAddress::of).orElse(null))
                .type(CCSDefendantType.of(defendant.getType()))
                .probationStatus(defendant.getProbationStatus())
                .offences(Optional.of(defendant)
                        .map(Defendant::getOffences)
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(CCSOffence::of)
                        .collect(Collectors.toList()))
                .crn(defendant.getCrn())
                .pnc(defendant.getPnc())
                .cro(defendant.getCro())
                .preSentenceActivity(defendant.getPreSentenceActivity())
                .suspendedSentenceOrder(defendant.getSuspendedSentenceOrder())
                .sex(defendant.getSex())
                .previouslyKnownTerminationDate(defendant.getPreviouslyKnownTerminationDate())
                .awaitingPsr(defendant.getAwaitingPsr())
                .breach(defendant.getBreach())
                .build();
    }

    private static String generateUUID() {
        return UUID.randomUUID().toString();
    }
}
