package uk.gov.justice.probation.courtcasematcher.pact;

import uk.gov.justice.probation.courtcasematcher.model.domain.Address;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

public class DomainDataHelper {
    public static final String DEFENDANT_ID = "8E07B58D-3ED3-440E-9CC2-2BC94EDBC5AF";
    public static String CASE_ID = "D517D32D-3C80-41E8-846E-D274DC2B94A5";

    public static CourtCase aCourtCaseWithAllFields() {
        return aCourtCaseBuilderWithAllFields()
                .build();
    }

    public static CourtCase.CourtCaseBuilder aCourtCaseBuilderWithAllFields() {
        return aMinimalCourtCaseBuilder()
                .caseNo("case no")
                .probationStatus("Current")
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
                .defendantName("defendant name")
                .defendantSex("defendant sex")
                .nationality1("nationality 1")
                .nationality2("nationality 2")
                .breach(true)
                .previouslyKnownTerminationDate(LocalDate.of(2021, 8, 25))
                .suspendedSentenceOrder(true)
                .preSentenceActivity(true)
                .awaitingPsr(true)
                .defendantAddress(Address.builder()
                        .line1("line1")
                        .line2("line2")
                        .line3("line3")
                        .line4("line4")
                        .line5("line5")
                        .postcode("S1 1AB")
                        .build())
                .defendantSex("sex");
    }

    public static CourtCase aMinimalValidCourtCase() {
        return aMinimalCourtCaseBuilder()
                .build();
    }

    public static CourtCase.CourtCaseBuilder aMinimalCourtCaseBuilder() {
        return CourtCase.builder()
                .source(DataSource.LIBRA)
                .caseId(CASE_ID)
                .defendantId(DEFENDANT_ID)
                .courtCode("B10JQ")
                .courtRoom("ROOM 1")
                .sessionStartTime(LocalDateTime.of(2021, 8, 26, 9, 0))
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
                .defendantAddress(Address.builder()
                        .line1("line1")
                        .build())
                .defendantDob(LocalDate.of(1986, 11, 28))
                .defendantType(DefendantType.PERSON)
                .listNo("1");
    }
}