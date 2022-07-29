package uk.gov.justice.probation.courtcasematcher.pact;

import uk.gov.justice.probation.courtcasematcher.model.domain.Address;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.type.DefendantType;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

public class DomainDataHelper {

    public static final String DEFENDANT_ID = "0ab7c3e5-eb4c-4e3f-b9e6-b9e78d3ea199";
    public static final String DEFENDANT_ID_2 = "903c4c54-f667-4770-8fdf-1adbb5957c25";
    public static String CASE_ID = "D517D32D-3C80-41E8-846E-D274DC2B94A5";
    public static String HEARING_ID = "ABCDD32D-3C80-41E8-846E-D274DC2B94A5";

    public static Hearing aCourtCaseWithAllFields() {
        return aCourtCaseBuilderWithAllFields()
                .build();
    }

    public static Hearing.HearingBuilder aCourtCaseBuilderWithAllFields() {
        return aMinimalCourtCaseBuilder()
                .caseNo("case no")
                .hearingId(HEARING_ID)
                .urn("urn")
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

    public static Hearing aMinimalValidCourtCase() {
        return aMinimalCourtCaseBuilder()
                .build();
    }

    public static Hearing.HearingBuilder aMinimalCourtCaseBuilder() {
        return Hearing.builder()
                .caseId(CASE_ID)
                .hearingId(HEARING_ID)
                .source(DataSource.COMMON_PLATFORM)
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
                        .sex("M")
                        .build()))
                ;
    }
}
