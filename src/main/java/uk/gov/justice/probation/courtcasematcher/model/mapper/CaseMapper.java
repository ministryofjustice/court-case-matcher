package uk.gov.justice.probation.courtcasematcher.model.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.Address;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.Offence;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Offender;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@Component
@Slf4j
public class CaseMapper {

    private final String defaultProbationStatus;

    public CaseMapper(@Value("${case-mapper-reference.defaultProbationStatus}") String defaultProbationStatus) {
        super();
        this.defaultProbationStatus = defaultProbationStatus;
    }


    public CourtCase newFromCase(Case aCase) {
        return getCourtCaseBuilderFromCase(aCase)
            .build();
    }

    private CourtCase.CourtCaseBuilder getCourtCaseBuilderFromCase(Case aCase) {
        return CourtCase.builder()
            .caseNo(aCase.getCaseNo())
            .courtCode(aCase.getBlock().getSession().getCourtCode())
            .caseId(String.valueOf(aCase.getId()))
            .courtRoom(aCase.getBlock().getSession().getCourtRoom())
            .defendantAddress(Optional.ofNullable(aCase.getDef_addr()).map(CaseMapper::fromAddress).orElse(null))
            .defendantName(aCase.getDef_name())
            .defendantDob(aCase.getDef_dob())
            .defendantSex(aCase.getDef_sex())
            .cro(aCase.getCro())
            .pnc(aCase.getPnc())
            .listNo(aCase.getListNo())
            .sessionStartTime(aCase.getBlock().getSession().getSessionStartTime())
            .probationStatus(defaultProbationStatus)
            .offences(Optional.ofNullable(aCase.getOffences()).map(CaseMapper::fromOffences).orElse(Collections.emptyList()));
    }

    private static List<Offence> fromOffences(List<uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Offence> offences) {
        return offences.stream()
            .sorted(comparing(uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Offence::getSeq))
            .map(offence -> Offence.builder()
                                .offenceTitle(offence.getTitle())
                                .offenceSummary(offence.getSum())
                                .sequenceNumber(offence.getSeq())
                                .act(offence.getAs())
                                .build())
            .collect(Collectors.toList());
    }

    public static Address fromAddress(uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Address def_addr) {
        return Address.builder()
            .line1(def_addr.getLine1())
            .line2(def_addr.getLine2())
            .line3(def_addr.getLine3())
            .line4(def_addr.getLine4())
            .line5(def_addr.getLine5())
            .postcode(def_addr.getPcode())
            .build();
    }

    public CourtCase merge(Case incomingCase, CourtCase existingCourtCase) {
        return CourtCase.builder()
            // PK fields
            .courtCode(incomingCase.getBlock().getSession().getCourtCode())
            .caseNo(existingCourtCase.getCaseNo())
            // Fields to be updated from incoming
            .caseId(String.valueOf(incomingCase.getId()))
            .courtRoom(incomingCase.getBlock().getSession().getCourtRoom())
            .defendantAddress(fromAddress(incomingCase.getDef_addr()))
            .defendantName(incomingCase.getDef_name())
            .defendantSex(incomingCase.getDef_sex())
            .defendantDob(incomingCase.getDef_dob())
            .listNo(incomingCase.getListNo())
            .sessionStartTime(incomingCase.getBlock().getSession().getSessionStartTime())
            .offences(fromOffences(incomingCase.getOffences()))
            // Fields to be retained from existing court case
            .breach(existingCourtCase.getBreach())
            .crn(existingCourtCase.getCrn())
            .probationStatus(existingCourtCase.getProbationStatus())
            .suspendedSentenceOrder(existingCourtCase.getSuspendedSentenceOrder())
            .pnc(existingCourtCase.getPnc())

            .build();
    }

    public CourtCase newFromCaseAndOffender(Case incomingCase, Offender offender, GroupedOffenderMatches groupedOffenderMatches) {
        return getCourtCaseBuilderFromCase(incomingCase)
                .probationStatus(defaultProbationStatus)
                .crn(offender.getOtherIds().getCrn())
                .cro(offender.getOtherIds().getCro())
                .pnc(offender.getOtherIds().getPnc())
                .groupedOffenderMatches(groupedOffenderMatches)
                .build();
    }

}
