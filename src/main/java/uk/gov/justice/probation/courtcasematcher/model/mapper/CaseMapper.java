package uk.gov.justice.probation.courtcasematcher.model.mapper;

import static java.util.Comparator.comparing;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.justice.probation.courtcasematcher.application.CaseMapperReference;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseserviceapi.AddressApi;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseserviceapi.CourtCaseApi;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseserviceapi.OffenceApi;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Address;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Offence;

@Component
public class CaseMapper {

    private final CaseMapperReference caseMapperReference;

    public CaseMapper(@Autowired CaseMapperReference caseMapperReference) {
        super();
        this.caseMapperReference = caseMapperReference;
    }

    public CourtCaseApi newFromCase(Case aCase) {
        return CourtCaseApi.builder()
            .caseNo(aCase.getCaseNo())
            .courtCode(aCase.getCourtCode())
            .caseId(String.valueOf(aCase.getId()))
            .courtRoom(aCase.getCourtRoom())
            .defendantAddress(Optional.ofNullable(aCase.getDef_addr()).map(CaseMapper::fromAddress).orElse(null))
            .defendantName(aCase.getDef_name())
            .defendantDob(aCase.getDef_dob())
            .defendantSex(aCase.getDef_sex())
            .listNo(aCase.getListNo())
            .nationality1(aCase.getNationality1())
            .nationality2(aCase.getNationality2())
            .sessionStartTime(aCase.getSessionStartTime())
            .probationStatus(caseMapperReference.getDefaultProbationStatus())
            .offences(Optional.ofNullable(aCase.getOffences()).map(CaseMapper::fromOffences).orElse(Collections.emptyList()))
            .build();
    }

    private static List<OffenceApi> fromOffences(List<Offence> offences) {
        return offences.stream()
            .sorted(comparing(Offence::getSeq))
            .map(offence -> OffenceApi.builder()
                                .offenceTitle(offence.getTitle())
                                .offenceSummary(offence.getSum())
                                .sequenceNumber(offence.getSeq())
                                .act(offence.getAs())
                                .build())
            .collect(Collectors.toList());
    }

    public static AddressApi fromAddress(Address def_addr) {
        return AddressApi.builder()
            .line1(def_addr.getLine1())
            .line2(def_addr.getLine2())
            .line3(def_addr.getLine3())
            .line4(def_addr.getLine4())
            .line5(def_addr.getLine5())
            .postcode(def_addr.getPcode())
            .build();
    }

    public CourtCaseApi merge(Case aCase, CourtCaseApi courtCaseApi) {
        courtCaseApi.setCourtRoom(aCase.getCourtRoom());
        courtCaseApi.setDefendantAddress(fromAddress(aCase.getDef_addr()));
        courtCaseApi.setDefendantName(aCase.getDef_name());
        courtCaseApi.setDefendantSex(aCase.getDef_sex());
        courtCaseApi.setDefendantDob(aCase.getDef_dob());
        courtCaseApi.setListNo(aCase.getListNo());
        courtCaseApi.setNationality1(aCase.getNationality1());
        courtCaseApi.setNationality2(aCase.getNationality2());
        courtCaseApi.setSessionStartTime(aCase.getSessionStartTime());
        courtCaseApi.setOffences(fromOffences(aCase.getOffences()));

        return courtCaseApi;
    }

}
