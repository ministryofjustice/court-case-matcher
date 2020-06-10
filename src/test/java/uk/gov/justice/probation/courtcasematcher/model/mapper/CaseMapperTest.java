package uk.gov.justice.probation.courtcasematcher.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.application.CaseMapperReference;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseserviceapi.CourtCaseApi;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseserviceapi.OffenceApi;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Address;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Offence;

class CaseMapperTest {

    private static final String DEFAULT_PROBATION_STATUS = "No record";

    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(1969, Month.AUGUST, 26);
    private static final LocalDateTime SESSION_START_TIME = LocalDateTime.of(2020, Month.FEBRUARY, 29, 9, 10);

    private Case aCase;

    private static CaseMapper caseMapper;

    @BeforeAll
    static void beforeAll() {
        CaseMapperReference caseMapperReference = new CaseMapperReference();
        caseMapperReference.setDefaultProbationStatus(DEFAULT_PROBATION_STATUS);
        caseMapperReference.setCourtNameToCodes(Map.of("SheffieldMagistratesCourt", "SHF"));
        caseMapper = new CaseMapper(caseMapperReference);
    }

    @BeforeEach
    void beforeEach() {
        aCase = Case.builder()
            .caseNo("123")
            .courtCode("SHF")
            .courtRoom("2")
            .def_addr(Address.builder().line1("line 1").line2("line 2").line3("line 3").pcode("LD1 1AA").build())
            .def_age("13")
            .def_dob(DATE_OF_BIRTH)
            .def_name("Mr James BLUNT")
            .def_sex("M")
            .def_type("C")
            .h_id(13442L)
            .id(321321L)
            .inf("POL01")
            .listNo("1st")
            .nationality1("British")
            .nationality2("Transylvania")
            .seq(1)
            .sessionStartTime(SESSION_START_TIME)
            .type("C")
            .valid("Y")
            .build();
    }

    @DisplayName("Map a new case from gateway case but with no offences")
    @Test
    void whenMapNewCaseThenCreateNewCaseNoOffences() {

        CourtCaseApi courtCaseApi = caseMapper.newFromCase(aCase);

        assertThat(courtCaseApi.getCaseNo()).isEqualTo("123");
        assertThat(courtCaseApi.getCaseId()).isEqualTo("321321");
        assertThat(courtCaseApi.getCourtCode()).isEqualTo("SHF");
        assertThat(courtCaseApi.getCourtRoom()).isEqualTo("2");
        assertThat(courtCaseApi.getProbationStatus()).isEqualTo(DEFAULT_PROBATION_STATUS);
        assertThat(courtCaseApi.getDefendantAddress().getLine1()).isEqualTo("line 1");
        assertThat(courtCaseApi.getDefendantAddress().getLine2()).isEqualTo("line 2");
        assertThat(courtCaseApi.getDefendantAddress().getLine3()).isEqualTo("line 3");
        assertThat(courtCaseApi.getDefendantAddress().getPostcode()).isEqualTo("LD1 1AA");
        assertThat(courtCaseApi.getDefendantDob()).isEqualTo(DATE_OF_BIRTH);
        assertThat(courtCaseApi.getDefendantName()).isEqualTo("Mr James BLUNT");
        assertThat(courtCaseApi.getDefendantSex()).isEqualTo("M");
        assertThat(courtCaseApi.getSessionStartTime()).isEqualTo(SESSION_START_TIME);
        assertThat(courtCaseApi.getOffences()).isEmpty();
    }

    @DisplayName("Map from a new case composed of nulls. Ensures no null pointers.")
    @Test
    void whenMapCaseWithNullsThenCreateNewCaseNoOffences_EnsureNoNullPointer() {
        assertThat(caseMapper.newFromCase(Case.builder().build())).isNotNull();
    }

    @DisplayName("Map from a new case with offences")
    @Test
    void whenMapCaseWithOffences_ThenCreateNewCase() {

        Offence offence1 = Offence.builder()
            .as("Contrary to section 2(2) and 8 of the Theft Act 1968.")
            .sum("On 02/02/2022 at Town, stole Article, to the value of £0.02, belonging to Person.")
            .code("ASD2537")
            .title("Theft from a person")
            .seq(1)
            .build();
        Offence offence2 = Offence.builder()
            .as("Contrary to section 1(1) and 7 of the Theft Act 1968.")
            .sum("On 01/01/2016 at Town, stole Article, to the value of £100.00, belonging to Shop.")
            .code("ASD2537")
            .title("Theft from a shop")
            .seq(2)
            .build();

        // Put Seq 2 first in list
        Case aCase = Case.builder()
            .caseNo("123")
            .offences(Arrays.asList(offence2, offence1))
            .build();

        CourtCaseApi courtCaseApi = caseMapper.newFromCase(aCase);

        assertThat(courtCaseApi.getOffences()).hasSize(2);
        OffenceApi offenceApi = courtCaseApi.getOffences().get(0);
        assertThat(offenceApi.getSequenceNumber()).isEqualTo(1);
        assertThat(offenceApi.getAct()).isEqualTo("Contrary to section 2(2) and 8 of the Theft Act 1968.");
        assertThat(offenceApi.getOffenceSummary()).isEqualTo("On 02/02/2022 at Town, stole Article, to the value of £0.02, belonging to Person.");
        assertThat(offenceApi.getOffenceTitle()).isEqualTo("Theft from a person");
    }

    @DisplayName("Merge the gateway case with the existing court case, including offences")
    @Test
    void whenMergeWithExistingCase_ThenUpdateExistingCase() {

        aCase.setOffences(Collections.singletonList(buildOffence("NEW Theft from a person", 1)));

        CourtCaseApi existingCourtCaseApi = CourtCaseApi.builder()
            .breach(Boolean.TRUE)
            .suspendedSentenceOrder(Boolean.FALSE)
            .crn("X320741")
            .pnc("PNC")
            .caseNo("12345")
            .caseId("123456")
            .probationStatus("Current")
            .courtCode("SHF")
            .defendantAddress(null)
            .defendantName("Pat Garrett")
            .defendantDob(LocalDate.of(1969, Month.JANUARY, 1))
            .nationality1("USA")
            .nationality2("Irish")
            .defendantSex("N")
            .listNo("999st")
            .courtRoom("4")
            .sessionStartTime(LocalDateTime.of(2020, Month.JANUARY, 3, 9, 10, 0))
            .offences(Collections.singletonList(OffenceApi.builder()
                                                        .act("act")
                                                        .sequenceNumber(1)
                                                        .offenceSummary("summary")
                                                        .offenceTitle("title")
                                                        .build()))
            .build();
        aCase.setNationality2(null);

        CourtCaseApi courtCaseApi = caseMapper.merge(aCase, existingCourtCaseApi);
        // Fields that stay the same on existing value
        assertThat(courtCaseApi.getCourtCode()).isEqualTo("SHF");
        assertThat(courtCaseApi.getCourtRoom()).isEqualTo("2");
        assertThat(courtCaseApi.getProbationStatus()).isEqualTo("Current");
        assertThat(courtCaseApi.getCaseNo()).isEqualTo("12345");
        assertThat(courtCaseApi.getCaseId()).isEqualTo("123456");
        assertThat(courtCaseApi.getBreach()).isTrue();
        // Fields that get overwritten from Libra incoming (even if null)
        assertThat(courtCaseApi.getDefendantAddress().getLine1()).isEqualTo("line 1");
        assertThat(courtCaseApi.getDefendantAddress().getLine2()).isEqualTo("line 2");
        assertThat(courtCaseApi.getDefendantAddress().getLine3()).isEqualTo("line 3");
        assertThat(courtCaseApi.getDefendantAddress().getPostcode()).isEqualTo("LD1 1AA");
        assertThat(courtCaseApi.getDefendantDob()).isEqualTo(DATE_OF_BIRTH);
        assertThat(courtCaseApi.getDefendantName()).isEqualTo("Mr James BLUNT");
        assertThat(courtCaseApi.getDefendantSex()).isEqualTo("M");
        assertThat(courtCaseApi.getSessionStartTime()).isEqualTo(SESSION_START_TIME);
        assertThat(courtCaseApi.getNationality1()).isEqualTo("British");
        assertThat(courtCaseApi.getNationality2()).isNull();
        assertThat(courtCaseApi.getOffences()).hasSize(1);
        assertThat(courtCaseApi.getOffences().get(0).getOffenceTitle()).isEqualTo("NEW Theft from a person");
        assertThat(courtCaseApi.getOffences().get(0).getSequenceNumber()).isEqualTo(1);
    }

    private Offence buildOffence(String title, Integer seq) {
        return Offence.builder()
            .as("Contrary to section 2(2) and 8 of the Theft Act 1968.")
            .sum("On 02/02/2022 at Town, stole Article, to the value of £0.02, belonging to Person.")
            .code("ASD2537")
            .title(title)
            .seq(seq)
            .build();
    }

}
