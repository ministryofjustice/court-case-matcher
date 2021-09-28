package uk.gov.justice.probation.courtcasematcher.pact;

import uk.gov.justice.probation.courtcasematcher.model.domain.Address;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

public class DomainDataHelper {
    public static final String DEFENDANT_ID = "8E07B58D-3ED3-440E-9CC2-2BC94EDBC5AF";
    public static final String DEFENDANT_ID_2 = "903c4c54-f667-4770-8fdf-1adbb5957c25";
    public static String CASE_ID = "D517D32D-3C80-41E8-846E-D274DC2B94A5";

    public static CourtCase aCourtCaseWithAllFields() {
        return aCourtCaseBuilderWithAllFields()
                .build();
    }

    public static CourtCase.CourtCaseBuilder aCourtCaseBuilderWithAllFields() {
        return aMinimalCourtCaseBuilder()
                .caseNo("case no")
                .defendants(Collections.singletonList(Defendant.builder()
                        .defendantId(DEFENDANT_ID)
                        .probationStatus("Current")
                        .offences(Collections.singletonList(Offence.builder()
                                .offenceTitle("offence title")
                                .offenceSummary("offence summary")
                                .act("offence act")
                                .sequenceNumber(1)
                                .build()))
                        .crn("crn")
                        .cro("cro")
                        .pnc("pnc")
                        .name(Name.builder()
                                .title("title")
                                .forename1("forename 1")
                                .forename2("forename 2")
                                .forename3("forename 3")
                                .surname("surname")
                                .build())
                        .breach(true)
                        .previouslyKnownTerminationDate(LocalDate.of(2021, 8, 25))
                        .dateOfBirth(LocalDate.of(1986, 11, 28))
                        .suspendedSentenceOrder(true)
                        .preSentenceActivity(true)
                        .awaitingPsr(true)
                        .address(Address.builder()
                                .line1("line1")
                                .line2("line2")
                                .line3("line3")
                                .line4("line4")
                                .line5("line5")
                                .postcode("S1 1AB")
                                .build())
                        .sex("sex")
                        .type(DefendantType.ORGANISATION)
                        .build()))
                ;
    }

    public static CourtCase aMinimalValidCourtCase() {
        return aMinimalCourtCaseBuilder()
                .build();
    }

    public static CourtCase.CourtCaseBuilder aMinimalCourtCaseBuilder() {
        return CourtCase.builder()
                .caseId(CASE_ID)
                .source(DataSource.LIBRA)
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("B10JQ")
                        .courtRoom("ROOM 1")
                        .sessionStartTime(LocalDateTime.of(2021, 8, 26, 9, 0))
                        .listNo("1")
                        .build()))
                .defendants(Collections.singletonList(Defendant.builder()
                        .defendantId(DEFENDANT_ID)
                        .offences(Collections.singletonList(Offence.builder()
                                .offenceTitle("offence title")
                                .offenceSummary("offence summary")
                                .act("offence act")
                                .sequenceNumber(1)
                                .build()))
                        .name(Name.builder()
                                .title("title")
                                .forename1("forename1")
                                .surname("surname")
                                .build())
                        .address(Address.builder()
                                .line1("line1")
                                .build())
                        .dateOfBirth(LocalDate.of(1986, 11, 28))
                        .type(DefendantType.PERSON)
                        .build()))
                ;
    }
}
