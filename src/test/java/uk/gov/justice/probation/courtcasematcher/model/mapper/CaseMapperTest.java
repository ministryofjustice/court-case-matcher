package uk.gov.justice.probation.courtcasematcher.model.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraAddress;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraCase;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraName;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraOffence;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType;
import uk.gov.justice.probation.courtcasematcher.model.domain.MatchIdentifiers;
import uk.gov.justice.probation.courtcasematcher.model.domain.MatchType;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;
import uk.gov.justice.probation.courtcasematcher.model.domain.OffenderMatch;
import uk.gov.justice.probation.courtcasematcher.model.domain.ProbationStatusDetail;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.Offender;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OffenderSearchMatchType;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OtherIds;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.SearchResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.probation.courtcasematcher.model.domain.DataSource.LIBRA;

class CaseMapperTest {

    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(1969, Month.AUGUST, 26);
    private static final LocalDate DATE_OF_HEARING = LocalDate.of(2020, Month.FEBRUARY, 29);
    private static final LocalTime START_TIME = LocalTime.of(9, 10);
    private static final LocalDateTime SESSION_START_TIME = LocalDateTime.of(2020, Month.FEBRUARY, 29, 9, 10);
    private static final String COURT_CODE = "B10JQ";

    private static final Name name = Name.builder().title("Mr")
                                                    .forename1("Patrick")
                                                    .forename2("Floyd")
                                                    .forename3("Jarvis")
                                                    .surname("Garrett")
                                                    .build();

    private static final LibraName libraName = LibraName.builder().title("Mr")
                                                    .forename1("Patrick")
                                                    .forename2("Floyd")
                                                    .forename3("Jarvis")
                                                    .surname("Garrett")
                                                    .build();
    public static final String CRN = "CRN123";
    public static final String CRO = "CRO456";
    public static final String PNC = "PNC789";
    private static final String A_UUID = "D517D32D-3C80-41E8-846E-D274DC2B94A5";
    private static final String DEFENDANT_ID = "9E27A145-E847-4AAB-9FF9-B88912520D14";
    private static final String CASE_ID = "8CC06F57-F5F7-4858-A9BF-035F7D6AC60F";

    private LibraCase aLibraCase;

    @BeforeEach
    void beforeEach() {

        aLibraCase = LibraCase.builder()
            .caseNo("123")
            .courtCode(COURT_CODE)
            .courtRoom("00")
            .defendantAddress(LibraAddress.builder().line1("line 1").line2("line 2").line3("line 3").pcode("LD1 1AA").build())
            .defendantAge("13")
            .defendantDob(DATE_OF_BIRTH)
            .name(libraName)
            .defendantSex("M")
            .defendantType("P")
            .listNo("1st")
            .seq(1)
            .offences(singletonList(buildOffence("NEW Theft from a person", 1)))
            .sessionStartTime(LocalDateTime.of(DATE_OF_HEARING, START_TIME))
            .build();

    }

    @DisplayName("New from an existing CourtCase, adding MatchDetails")
    @Nested
    class NewFromCourtCaseWithMatches {

        @DisplayName("Map a court case to a new court case when search response has yielded no matches")
        @Test
        void givenNoMatches_whenMapNewFromCaseAndSearchResponse_thenCreateNewCaseWithEmptyListOfMatches() {

            var courtCase = CaseMapper.newFromLibraCase(aLibraCase);
            var matchResponse = MatchResponse.builder()
                .matchedBy(OffenderSearchMatchType.NOTHING)
                .build();

            var courtCaseNew = CaseMapper.newFromCourtCaseWithMatches(courtCase, buildMatchDetails(matchResponse));

            assertThat(courtCaseNew).isNotSameAs(courtCase);
            assertThat(courtCaseNew.getCrn()).isNull();
            assertThat(courtCaseNew.getGroupedOffenderMatches().getMatches()).hasSize(0);
            assertThat(courtCaseNew.getCaseId()).isEqualTo(courtCase.getCaseId());
            assertThat(courtCaseNew.getDefendantId()).isEqualTo(courtCase.getDefendantId());
        }

        @DisplayName("Map a court case to a new court case when search response has yielded a single match")
        @Test
        void givenSingleMatch_whenMapNewFromCaseAndSearchResponse_thenCreateNewCaseWithSingleMatch() {
            var match = Match.builder()
                .offender(Offender.builder()
                    .otherIds(OtherIds.builder().crn(CRN).croNumber(CRO).pncNumber(PNC).build())
                    .probationStatus(ProbationStatusDetail.builder().status("CURRENT").preSentenceActivity(true).awaitingPsr(true).build())
                    .build())
                .build();

            var courtCase = CaseMapper.newFromLibraCase(aLibraCase);
            var matchResponse = MatchResponse.builder()
                .matchedBy(OffenderSearchMatchType.ALL_SUPPLIED)
                .matches(List.of(match))
                .build();

            var courtCaseNew = CaseMapper.newFromCourtCaseWithMatches(courtCase, buildMatchDetails(matchResponse));

            assertThat(courtCaseNew).isNotSameAs(courtCase);
            assertThat(courtCaseNew.getCrn()).isEqualTo(CRN);
            assertThat(courtCaseNew.getPnc()).isEqualTo(PNC);
            assertThat(courtCaseNew.getProbationStatus()).isEqualTo("CURRENT");
            assertThat(courtCaseNew.getPreviouslyKnownTerminationDate()).isNull();
            assertThat(courtCaseNew.getBreach()).isNull();
            assertThat(courtCaseNew.isPreSentenceActivity()).isTrue();
            assertThat(courtCaseNew.isAwaitingPsr()).isTrue();
            assertThat(courtCaseNew.getGroupedOffenderMatches().getMatches()).hasSize(1);
            OffenderMatch offenderMatch1 = buildOffenderMatch(MatchType.NAME_DOB, CRN, CRO, PNC);
            assertThat(courtCaseNew.getGroupedOffenderMatches().getMatches()).containsExactly(offenderMatch1);
        }

        @DisplayName("Map a court case to a new court case when search response has yielded a single non-exact match")
        @Test
        void givenSingleMatchOnNameButNonExact_whenMapNewFromCaseAndSearchResponse_thenCreateNewCaseWithSingleMatchButNoCrn() {
            var previouslyKnownTerminationDate = LocalDate.of(2016, Month.DECEMBER, 25);
            var match = Match.builder().offender(Offender.builder()
                .otherIds(OtherIds.builder().crn(CRN).croNumber(CRO).pncNumber(PNC).build())
                .probationStatus(ProbationStatusDetail.builder()
                                        .awaitingPsr(true)
                                        .preSentenceActivity(true)
                                        .inBreach(Boolean.FALSE)
                                        .previouslyKnownTerminationDate(previouslyKnownTerminationDate)
                                        .status("PREVIOUSLY_KNOWN").build())
                .build())
                .build();

            var courtCase = CaseMapper.newFromLibraCase(aLibraCase);
            var matchResponse = MatchResponse.builder()
                .matchedBy(OffenderSearchMatchType.NAME)
                .matches(List.of(match))
                .build();

            var courtCaseNew = CaseMapper.newFromCourtCaseWithMatches(courtCase, buildMatchDetails(matchResponse));

            assertThat(courtCaseNew).isNotSameAs(courtCase);
            assertThat(courtCaseNew.getCrn()).isNull();
            // The new probation status details are all ignored because match is non-exact
            assertThat(courtCaseNew.getProbationStatus()).isNull();
            assertThat(courtCaseNew.getBreach()).isNull();
            assertThat(courtCaseNew.getPreviouslyKnownTerminationDate()).isNull();
            assertThat(courtCaseNew.isPreSentenceActivity()).isFalse();
            assertThat(courtCaseNew.isAwaitingPsr()).isFalse();
            assertThat(courtCaseNew.getGroupedOffenderMatches().getMatches()).hasSize(1);
            var expectedOffenderMatch = buildOffenderMatch(MatchType.NAME, CRN, CRO, PNC);
            assertThat(courtCaseNew.getGroupedOffenderMatches().getMatches()).containsExactly(expectedOffenderMatch);
        }

        @DisplayName("Map a court case to a new court case when search response has yielded a single match but null probation status")
        @Test
        void givenSingleMatchWithNoProbationStatus_whenMapNewFromCaseAndSearchResponse_thenCreateNewCaseWithSingleMatch() {
            var match = Match.builder().offender(Offender.builder()
                .otherIds(OtherIds.builder().crn(CRN).croNumber(CRO).pncNumber(PNC).build())
                .build())
                .build();

            var courtCase = CaseMapper.newFromLibraCase(aLibraCase);
            var matchResponse = MatchResponse.builder()
                .matchedBy(OffenderSearchMatchType.ALL_SUPPLIED)
                .matches(List.of(match))
                .build();

            var courtCaseNew = CaseMapper.newFromCourtCaseWithMatches(courtCase, buildMatchDetails(matchResponse));

            assertThat(courtCaseNew).isNotSameAs(courtCase);
            assertThat(courtCaseNew.getCrn()).isEqualTo(CRN);
            assertThat(courtCaseNew.getProbationStatus()).isNull();
            assertThat(courtCaseNew.getBreach()).isNull();
            assertThat(courtCaseNew.getPreviouslyKnownTerminationDate()).isNull();
            assertThat(courtCaseNew.getGroupedOffenderMatches().getMatches()).hasSize(1);
            var offenderMatch1 = buildOffenderMatch(MatchType.NAME_DOB, CRN, CRO, PNC);
            assertThat(courtCaseNew.getGroupedOffenderMatches().getMatches()).containsExactly(offenderMatch1);
        }

        @DisplayName("Map a court case to a new court case when search response has yielded multiple matches")
        @Test
        void givenMultipleMatches_whenMapNewFromCaseAndSearchResponse_thenCreateNewCaseWithListOfMatches() {
            var match1 = Match.builder().offender(Offender.builder()
                .otherIds(OtherIds.builder().crn(CRN).croNumber(CRO).pncNumber(PNC).build())
                .build())
                .build();
            var match2 = Match.builder().offender(Offender.builder()
                .otherIds(OtherIds.builder().crn("CRN1").build())
                .build())
                .build();

            var courtCase = CaseMapper.newFromLibraCase(aLibraCase);
            var matchResponse = MatchResponse.builder()
                .matchedBy(OffenderSearchMatchType.PARTIAL_NAME)
                .matches(List.of(match1, match2))
                .build();

            var courtCaseNew = CaseMapper.newFromCourtCaseWithMatches(courtCase, buildMatchDetails(matchResponse));

            assertThat(courtCaseNew).isNotSameAs(courtCase);
            assertThat(courtCaseNew.getCrn()).isNull();
            assertThat(courtCaseNew.getProbationStatus()).isNull();
            assertThat(courtCaseNew.getBreach()).isNull();
            assertThat(courtCaseNew.getPreviouslyKnownTerminationDate()).isNull();
            assertThat(courtCaseNew.getGroupedOffenderMatches().getMatches()).hasSize(2);
            var offenderMatch1 = buildOffenderMatch(MatchType.PARTIAL_NAME, CRN, CRO, PNC);
            var offenderMatch2 = buildOffenderMatch(MatchType.PARTIAL_NAME, "CRN1", null, null);
            assertThat(courtCaseNew.getGroupedOffenderMatches().getMatches()).containsExactlyInAnyOrder(offenderMatch1, offenderMatch2);
        }

        private MatchDetails buildMatchDetails(MatchResponse matchResponse) {
            return MatchDetails.builder()
                .matchType(OffenderSearchMatchType.domainMatchTypeOf(SearchResult.builder()
                    .matchResponse(matchResponse)
                    .build()))
                .matches(matchResponse.getMatches())
                .exactMatch(matchResponse.isExactMatch())
                .build();
        }

        private OffenderMatch buildOffenderMatch(MatchType matchType, String crn, String cro, String pnc) {
            return OffenderMatch.builder()
                .matchType(matchType)
                .confirmed(false)
                .rejected(false)
                .matchIdentifiers(MatchIdentifiers.builder().pnc(pnc).cro(cro).crn(crn).build())
                .build();
        }
    }

    @DisplayName("New from incoming JSON case")
    @Nested
    class NewFromIncomingLibraCase {

        @DisplayName("Map from a new JSON case (with no block) composed of nulls. Ensures no null pointers.")
        @Test
        void givenJsonCase_whenMapCaseWithNullsThenCreateNewCaseNoOffences_EnsureNoNullPointer() {
            var nullCase = LibraCase.builder()
                .courtCode(COURT_CODE)
                .courtRoom("00")
                .sessionStartTime(LocalDateTime.of(DATE_OF_HEARING, START_TIME))
                .caseNo("123")
                .build();
            final var actual = CaseMapper.newFromLibraCase(nullCase);
            assertThat(actual).isNotNull();
            assertThat(actual.getSource()).isEqualTo(LIBRA);
        }

        @DisplayName("Generate UUIDs")
        @Test
        void givenLibraCase_whenNewFromLibraCase_thenGenerateIdsAndSource() {
            var nullCase = LibraCase.builder()
                .courtCode(COURT_CODE)
                .courtRoom("00")
                .sessionStartTime(LocalDateTime.of(DATE_OF_HEARING, START_TIME))
                .caseNo("123")
                .build();
            final var actual = CaseMapper.newFromLibraCase(nullCase);
            assertThat(actual.getCaseId()).hasSameSizeAs(A_UUID);
            assertThat(actual.getDefendantId()).hasSameSizeAs(A_UUID);
            assertThat(actual.getSource()).isEqualTo(LIBRA);
        }


        @DisplayName("Map from a new case with offences")
        @Test
        void whenMapCaseWithOffences_ThenCreateNewCase() {

            var offence1 = LibraOffence
                .builder()
                .act("Contrary to section 2(2) and 8 of the Theft Act 1968.")
                .summary("On 02/02/2022 at Town, stole Article, to the value of £0.02, belonging to Person.")
                .title("Theft from a person")
                .seq(1)
                .build();
            var offence2 = LibraOffence
                .builder()
                .act("Contrary to section 1(1) and 7 of the Theft Act 1968.")
                .summary("On 01/01/2016 at Town, stole Article, to the value of £100.00, belonging to Shop.")
                .title("Theft from a shop")
                .seq(2)
                .build();

            // Put Seq 2 first in list
            var aCase = LibraCase.builder()
                .caseNo("123")
                .offences(Arrays.asList(offence2, offence1))
                .build();

            var courtCase = CaseMapper.newFromLibraCase(aCase);

            assertThat(courtCase.getOffences()).hasSize(2);
            var offence = courtCase.getOffences().get(0);
            assertThat(offence.getSequenceNumber()).isEqualTo(1);
            assertThat(offence.getAct()).isEqualTo("Contrary to section 2(2) and 8 of the Theft Act 1968.");
            assertThat(offence.getOffenceSummary()).isEqualTo("On 02/02/2022 at Town, stole Article, to the value of £0.02, belonging to Person.");
            assertThat(offence.getOffenceTitle()).isEqualTo("Theft from a person");
        }
    }

    @DisplayName("Merge incoming gateway or JSON case to existing CourtCase")
    @Nested
    class MergeIncomingToExistingCourtCase {

        private CourtCase existingCourtCase;

        // A case created from a flatted incoming JSON structure with no parent block as we find in XML
        private CourtCase libraCase;

        @BeforeEach
        void beforeEach() {
            existingCourtCase = CourtCase.builder()
                .breach(Boolean.TRUE)
                .suspendedSentenceOrder(Boolean.TRUE)
                .crn("X320741")
                .pnc("PNC")
                .caseNo("12345")
                .probationStatus("CURRENT")
                .courtCode(COURT_CODE)
                .courtRoom("01")
                .defendantAddress(null)
                .defendantName("Pat Garrett")
                .defendantType(DefendantType.ORGANISATION)
                .defendantDob(LocalDate.of(1969, Month.JANUARY, 1))
                .nationality1("USA")
                .nationality2("Irish")
                .defendantSex("N")
                .listNo("999st")
                .courtRoom("4")
                .previouslyKnownTerminationDate(LocalDate.of(2001, Month.AUGUST, 26))
                .sessionStartTime(LocalDateTime.of(2020, Month.JANUARY, 3, 9, 10, 0))
                .offences(singletonList(Offence.builder()
                    .act("act")
                    .sequenceNumber(1)
                    .offenceSummary("summary")
                    .offenceTitle("title")
                    .build()))
                .build();

            libraCase = CourtCase.builder()
                .caseId(CASE_ID)
                .defendantId(DEFENDANT_ID)
                .caseNo("123")
                .defendantAddress(uk.gov.justice.probation.courtcasematcher.model.domain.Address.builder().line1("line 1").line2("line 2").line3("line 3").postcode("LD1 1AA").build())
                .defendantDob(DATE_OF_BIRTH)
                .name(name)
                .defendantSex("M")
                .defendantType(DefendantType.PERSON)
                .listNo("1st")
                .offences(singletonList(buildOffenceDomain("NEW Theft from a person", 1)))
                .courtCode(COURT_CODE)
                .courtRoom("00")
                .sessionStartTime(LocalDateTime.of(DATE_OF_HEARING, START_TIME))
                .build();
        }

        @DisplayName("Merge the JSON case with the existing court case, including offences")
        @Test
        void givenCaseFromJson_whenMergeWithExistingCase_ThenUpdateExistingCase() {

            ReflectionTestUtils.setField(libraCase, "defendantDob", null);

            var courtCase = CaseMapper.merge(libraCase, existingCourtCase);
            assertCourtCase(courtCase);

        }

        @DisplayName("Use existing defendantId and caseId if present")
        @Test
        void givenLibraCase_whenMergeWithExistingCasewithIds_ThenUseExistingIds() {

            ReflectionTestUtils.setField(libraCase, "defendantDob", null);

            final var existingCaseId = "82034D44-B709-4227-9CF9-CBFC67F98041";
            final var existingDefendantId = "C09C6A23-0390-41BB-948C-08399BD72720";
            final var existingCourtCase = CourtCase.builder()
                    .caseId(existingCaseId)
                    .defendantId(existingDefendantId)
                    .build();

            var courtCase = CaseMapper.merge(libraCase, existingCourtCase);

            assertThat(courtCase.getCaseId()).isEqualTo(existingCaseId);
            assertThat(courtCase.getDefendantId()).isEqualTo(existingDefendantId);

        }

        @DisplayName("Use new defendantId and caseId if existing not present")
        @Test
        void givenLibraCase_whenMergeWithExistingCaseWithoutIds_ThenUseNewIds() {

            ReflectionTestUtils.setField(libraCase, "defendantDob", null);

            final var existingCourtCase = CourtCase.builder()
                    .caseId(null)
                    .defendantId(null)
                    .build();

            var courtCase = CaseMapper.merge(libraCase, existingCourtCase);

            assertThat(courtCase.getCaseId()).isEqualTo(CASE_ID);
            assertThat(courtCase.getDefendantId()).isEqualTo(DEFENDANT_ID);
        }

        private void assertCourtCase(CourtCase courtCase) {
            // Fields that stay the same on existing value
            assertThat(courtCase.getCourtCode()).isEqualTo(COURT_CODE);
            assertThat(courtCase.getProbationStatus()).isEqualTo("CURRENT");
            assertThat(courtCase.getCaseNo()).isEqualTo("12345");
            assertThat(courtCase.getBreach()).isTrue();
            assertThat(courtCase.getSuspendedSentenceOrder()).isTrue();
            assertThat(courtCase.getCrn()).isEqualTo("X320741");
            assertThat(courtCase.getPnc()).isEqualTo("PNC");
            // Fields that get overwritten from Libra incoming (even if null)
            assertThat(courtCase.getCaseId()).isEqualTo(CASE_ID);
            assertThat(courtCase.getCourtRoom()).isEqualTo("00");
            assertThat(courtCase.getDefendantAddress().getLine1()).isEqualTo("line 1");
            assertThat(courtCase.getDefendantAddress().getLine2()).isEqualTo("line 2");
            assertThat(courtCase.getDefendantAddress().getLine3()).isEqualTo("line 3");
            assertThat(courtCase.getDefendantAddress().getPostcode()).isEqualTo("LD1 1AA");
            assertThat(courtCase.getDefendantDob()).isNull();
            assertThat(courtCase.getDefendantName()).isEqualTo("Mr Patrick Floyd Jarvis Garrett");
            assertThat(courtCase.getName()).isEqualTo(name);
            assertThat(courtCase.getDefendantType()).isSameAs(DefendantType.PERSON);
            assertThat(courtCase.getDefendantSex()).isEqualTo("M");
            assertThat(courtCase.getSessionStartTime()).isEqualTo(SESSION_START_TIME);
            assertThat(courtCase.getNationality1()).isNull();
            assertThat(courtCase.getNationality2()).isNull();
            assertThat(courtCase.getPreviouslyKnownTerminationDate()).isEqualTo(LocalDate.of(2001, Month.AUGUST, 26));
            assertThat(courtCase.getOffences()).hasSize(1);
            assertThat(courtCase.getOffences().get(0).getOffenceTitle()).isEqualTo("NEW Theft from a person");
            assertThat(courtCase.getOffences().get(0).getSequenceNumber()).isEqualTo(1);
        }
    }

    @DisplayName("Merge ProbationStatusDetail to existing CourtCase")
    @Nested
    class MergeProbationStatusDetailToExistingCourtCase {

        @DisplayName("Merge the gateway case with the existing court case, including offences")
        @Test
        void whenMergeWithExistingCase_ThenUpdateExistingCase() {

            var existingCourtCase = CourtCase.builder()
                .caseId(CASE_ID)
                .defendantId(DEFENDANT_ID)
                .breach(Boolean.TRUE)
                .suspendedSentenceOrder(Boolean.TRUE)
                .crn(CRN)
                .pnc(PNC)
                .cro(CRO)
                .caseNo("12345")
                .courtCode(COURT_CODE)
                .defendantAddress(null)
                .defendantName("Pat Garrett")
                .defendantType(DefendantType.PERSON)
                .defendantDob(LocalDate.of(1969, Month.JANUARY, 1))
                .name(Name.builder().forename1("Pat").surname("Garrett").build())
                .nationality1("USA")
                .nationality2("Irish")
                .defendantSex("N")
                .listNo("999st")
                .courtRoom("4")
                .sessionStartTime(LocalDateTime.of(2020, Month.JANUARY, 3, 9, 10, 0))
                .offences(singletonList(Offence.builder()
                    .act("act")
                    .sequenceNumber(1)
                    .offenceSummary("summary")
                    .offenceTitle("title")
                    .build()))
                .preSentenceActivity(false)
                .probationStatus("NOT_SENTENCED")
                .breach(false)
                .awaitingPsr(false)
                .previouslyKnownTerminationDate(LocalDate.of(2001, Month.AUGUST, 26))
                .build();

            var nextPrevKnownTermDate = existingCourtCase.getPreviouslyKnownTerminationDate().plusDays(1);
            var probationStatusDetail = ProbationStatusDetail.builder()
                .preSentenceActivity(true)
                .inBreach(Boolean.TRUE)
                .previouslyKnownTerminationDate(nextPrevKnownTermDate)
                .status("CURRENT")
                .awaitingPsr(true)
                .build();

            var courtCase = CaseMapper.merge(probationStatusDetail, existingCourtCase);

            assertThat(courtCase).isNotSameAs(existingCourtCase);

            // Fields that are updated
            assertThat(courtCase.getProbationStatus()).isEqualTo("CURRENT");
            assertThat(courtCase.getPreviouslyKnownTerminationDate()).isEqualTo(nextPrevKnownTermDate);
            assertThat(courtCase.getBreach()).isTrue();
            assertThat(courtCase.isPreSentenceActivity()).isTrue();
            assertThat(courtCase.isAwaitingPsr()).isTrue();
            // Fields that stay the same on existing value
            assertThat(courtCase.getCaseId()).isEqualTo(existingCourtCase.getCaseId());
            assertThat(courtCase.getDefendantId()).isEqualTo(existingCourtCase.getDefendantId());
            assertThat(courtCase.getCaseNo()).isEqualTo(existingCourtCase.getCaseNo());
            assertThat(courtCase.getCourtCode()).isEqualTo(existingCourtCase.getCourtCode());
            assertThat(courtCase.getCourtRoom()).isEqualTo(existingCourtCase.getCourtRoom());
            assertThat(courtCase.getCrn()).isEqualTo(existingCourtCase.getCrn());
            assertThat(courtCase.getCro()).isEqualTo(existingCourtCase.getCro());
            assertThat(courtCase.getDefendantAddress()).isEqualTo(existingCourtCase.getDefendantAddress());
            assertThat(courtCase.getDefendantDob()).isEqualTo(existingCourtCase.getDefendantDob());
            assertThat(courtCase.getDefendantName()).isEqualTo(existingCourtCase.getDefendantName());
            assertThat(courtCase.getDefendantSex()).isEqualTo(existingCourtCase.getDefendantSex());
            assertThat(courtCase.getDefendantType()).isSameAs(existingCourtCase.getDefendantType());
            assertThat(courtCase.getListNo()).isEqualTo(existingCourtCase.getListNo());
            assertThat(courtCase.getName()).isEqualTo(existingCourtCase.getName());
            assertThat(courtCase.getNationality1()).isEqualTo(existingCourtCase.getNationality1());
            assertThat(courtCase.getNationality2()).isEqualTo(existingCourtCase.getNationality2());
            assertThat(courtCase.getPnc()).isEqualTo(existingCourtCase.getPnc());
            assertThat(courtCase.getSessionStartTime()).isEqualTo(existingCourtCase.getSessionStartTime());
            assertThat(courtCase.getSuspendedSentenceOrder()).isEqualTo(existingCourtCase.getSuspendedSentenceOrder());
            assertThat(courtCase.getOffences()).hasSize(1);
            assertThat(courtCase.getOffences().get(0).getOffenceTitle()).isEqualTo("title");
            assertThat(courtCase.getOffences().get(0).getSequenceNumber()).isEqualTo(1);
        }
    }

    private uk.gov.justice.probation.courtcasematcher.model.domain.Offence buildOffenceDomain(String title, Integer seq) {
        return uk.gov.justice.probation.courtcasematcher.model.domain.Offence.builder()
            .act("Contrary to section 2(2) and 8 of the Theft Act 1968.")
            .offenceSummary("On 02/02/2022 at Town, stole Article, to the value of £0.02, belonging to Person.")
            .offenceTitle(title)
            .sequenceNumber(seq)
            .build();
    }

    private LibraOffence buildOffence(String title, Integer seq) {
        return LibraOffence.builder()
            .act("Contrary to section 2(2) and 8 of the Theft Act 1968.")
            .summary("On 02/02/2022 at Town, stole Article, to the value of £0.02, belonging to Person.")
            .title(title)
            .build();
    }

}
