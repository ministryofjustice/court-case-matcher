package uk.gov.justice.probation.courtcasematcher.model.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraAddress;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraHearing;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraName;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraOffence;
import uk.gov.justice.probation.courtcasematcher.model.domain.*;
import uk.gov.justice.probation.courtcasematcher.model.type.DefendantType;
import uk.gov.justice.probation.courtcasematcher.model.type.MatchType;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.probation.courtcasematcher.model.domain.DataSource.COMMON_PLATFORM;
import static uk.gov.justice.probation.courtcasematcher.model.domain.DataSource.LIBRA;

class HearingMapperTest {

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
    public static final String OFFENDER_SEARCH_CRO = "CRO456";
    public static final String OFFENDER_SEARCH_PNC = "PNC789";
    private static final String DEFENDANT_ID = "9E27A145-E847-4AAB-9FF9-B88912520D14";
    private static final String DEFENDANT_ID_2 = "F01ADD33-C534-44A6-BD7B-F2AAD0FB750C";
    private static final String CASE_ID = "8CC06F57-F5F7-4858-A9BF-035F7D6AC60F";

    private static List<OffenderAlias> offenderAliases = List.of(OffenderAlias.builder()
            .dateOfBirth(LocalDate.of(2000, 01, 01))
            .firstName("firstNameOne")
            .middleNames(List.of("middleOne", "middleTwo"))
            .surname("surnameOne")
            .gender("Not Specified")
            .build());

    private LibraHearing aLibraHearing;

    @BeforeEach
    void beforeEach() {

        aLibraHearing = LibraHearing.builder()
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

    @DisplayName("New from an existing Defendant, adding match details")
    @Nested
    class NewFromDefendantWithMatches {

        @DisplayName("Map a defendant to a new defendant when search response has yielded no matches")
        @Test
        void givenNoMatches_whenMapNewFromDefendantAndSearchResponse_thenCreateNewDefendantWithEmptyListOfMatches() {

            var defendant = HearingMapper.newFromLibraHearing(aLibraHearing)
                    .getDefendants()
                    .get(0);
            var matchResponse = MatchResponse.builder()
                    .matchedBy(OffenderSearchMatchType.NOTHING)
                    .build();

            var newDefendant = HearingMapper.updateDefendantWithMatches(defendant, matchResponse);

            assertThat(newDefendant).isNotSameAs(defendant);
            assertThat(newDefendant.getCrn()).isNull();
            assertThat(newDefendant.getGroupedOffenderMatches().getMatches()).hasSize(0);
            assertThat(newDefendant.getOffender()).isNull();
            assertThat(newDefendant.getDefendantId()).isEqualTo(defendant.getDefendantId());
        }

        @DisplayName("Map a defendant to a new defendant when search response has yielded a single match")
        @Test
        void givenSingleMatch_whenMapNewFromDefendantAndSearchResponse_thenCreateNewDefendantWithSingleMatch() {
            var match = Match.builder()
                    .offender(OSOffender.builder()
                            .otherIds(OtherIds.builder().crn(CRN).croNumber(OFFENDER_SEARCH_CRO).pncNumber(OFFENDER_SEARCH_PNC).build())
                            .offenderAliases(offenderAliases)
                            .probationStatus(ProbationStatusDetail.builder().status("CURRENT").preSentenceActivity(true).awaitingPsr(true).build())
                            .build())
                    .matchProbability(Mono.just(0.91d))
                    .build();

            var defendant = HearingMapper.newFromLibraHearing(aLibraHearing)
                    .getDefendants()
                    .get(0);
            var matchResponse = MatchResponse.builder()
                    .matchedBy(OffenderSearchMatchType.ALL_SUPPLIED)
                    .matches(List.of(match))
                    .build();

            final var newDefendant = HearingMapper.updateDefendantWithMatches(defendant, matchResponse);

            assertThat(newDefendant).isNotSameAs(defendant);
            assertThat(newDefendant.getCrn()).isEqualTo(CRN);
            assertThat(newDefendant.getPnc()).isNull();
            assertThat(newDefendant.getCro()).isNull();
            assertThat(newDefendant.getOffender().getPnc()).isEqualTo(OFFENDER_SEARCH_PNC);
            assertThat(newDefendant.getOffender().getCro()).isEqualTo(OFFENDER_SEARCH_CRO);
            assertThat(newDefendant.getProbationStatus()).isEqualTo("CURRENT");
            assertThat(newDefendant.getPreviouslyKnownTerminationDate()).isNull();
            assertThat(newDefendant.getBreach()).isNull();
            assertThat(newDefendant.getPreSentenceActivity()).isTrue();
            assertThat(newDefendant.getAwaitingPsr()).isTrue();
            assertThat(newDefendant.getGroupedOffenderMatches().getMatches()).hasSize(1);
            OffenderMatch offenderMatch1 = buildOffenderMatch(MatchType.NAME_DOB, CRN, OFFENDER_SEARCH_CRO, OFFENDER_SEARCH_PNC, offenderAliases, Mono.just(0.91));
            assertThat(newDefendant.getGroupedOffenderMatches().getMatches().get(0))
                    .usingRecursiveComparison()
                    .ignoringFields("matchProbability")
                    .isEqualTo(offenderMatch1);
            assertThat(newDefendant.getGroupedOffenderMatches().getMatches().get(0).getMatchProbability().block()).isEqualTo(0.91);
        }

        @DisplayName("Map a defendant to a new defendant when search response has yielded a single non-exact match")
        @Test
        void givenSingleMatchOnNameButNonExact_whenMapNewFromDefendantAndSearchResponse_thenCreateNewDefendantWithSingleMatchButNoCrn() {
            var previouslyKnownTerminationDate = LocalDate.of(2016, Month.DECEMBER, 25);
            var match = Match.builder().offender(OSOffender.builder()
                            .otherIds(OtherIds.builder().crn(CRN).croNumber(OFFENDER_SEARCH_CRO).pncNumber(OFFENDER_SEARCH_PNC).build())
                            .offenderAliases(offenderAliases)
                            .probationStatus(ProbationStatusDetail.builder()
                                    .awaitingPsr(true)
                                    .preSentenceActivity(true)
                                    .inBreach(Boolean.FALSE)
                                    .previouslyKnownTerminationDate(previouslyKnownTerminationDate)
                                    .status("PREVIOUSLY_KNOWN").build())
                            .build())
                    .build();

            var defendant = HearingMapper.newFromLibraHearing(aLibraHearing)
                    .getDefendants()
                    .get(0);
            var matchResponse = MatchResponse.builder()
                    .matchedBy(OffenderSearchMatchType.NAME)
                    .matches(List.of(match))
                    .build();

            var newDefendant = HearingMapper.updateDefendantWithMatches(defendant, matchResponse);

            assertThat(newDefendant).isNotSameAs(defendant);
            assertThat(newDefendant.getCrn()).isNull();
            assertThat(newDefendant.getPnc()).isNull();
            assertThat(newDefendant.getCro()).isNull();
            assertThat(newDefendant.getOffender()).isNull();
            // The new probation status details are all ignored because match is non-exact
            assertThat(newDefendant.getProbationStatus()).isNull();
            assertThat(newDefendant.getBreach()).isNull();
            assertThat(newDefendant.getPreviouslyKnownTerminationDate()).isNull();
            assertThat(newDefendant.getPreSentenceActivity()).isNull();
            assertThat(newDefendant.getAwaitingPsr()).isNull();
            assertThat(newDefendant.getGroupedOffenderMatches().getMatches()).hasSize(1);
            var expectedOffenderMatch = buildOffenderMatch(MatchType.NAME, CRN, OFFENDER_SEARCH_CRO, OFFENDER_SEARCH_PNC, offenderAliases);
            assertThat(newDefendant.getGroupedOffenderMatches().getMatches()).containsExactly(expectedOffenderMatch);
        }

        @DisplayName("Map a defendant to a new defendant when search response has yielded a single match but null probation status")
        @Test
        void givenSingleMatchWithNoProbationStatus_whenMapNewFromDefendantAndSearchResponse_thenCreateNewDefendantWithSingleMatch() {
            var match = Match.builder().offender(OSOffender.builder()
                            .otherIds(OtherIds.builder().crn(CRN).croNumber(OFFENDER_SEARCH_CRO).pncNumber(OFFENDER_SEARCH_PNC).build())
                            .offenderAliases(offenderAliases)
                            .build())
                    .build();

            var defendant = HearingMapper.newFromLibraHearing(aLibraHearing)
                    .getDefendants()
                    .get(0);
            var matchResponse = MatchResponse.builder()
                    .matchedBy(OffenderSearchMatchType.ALL_SUPPLIED)
                    .matches(List.of(match))
                    .build();

            var newDefendant = HearingMapper.updateDefendantWithMatches(defendant, matchResponse);

            assertThat(newDefendant).isNotSameAs(defendant);
            assertThat(newDefendant.getCrn()).isEqualTo(CRN);
            assertThat(newDefendant.getPnc()).isNull();
            assertThat(newDefendant.getCro()).isNull();
            assertThat(newDefendant.getOffender().getPnc()).isEqualTo(OFFENDER_SEARCH_PNC);
            assertThat(newDefendant.getOffender().getCro()).isEqualTo(OFFENDER_SEARCH_CRO);
            assertThat(newDefendant.getProbationStatus()).isNull();
            assertThat(newDefendant.getBreach()).isNull();
            assertThat(newDefendant.getPreviouslyKnownTerminationDate()).isNull();
            assertThat(newDefendant.getGroupedOffenderMatches().getMatches()).hasSize(1);
            var offenderMatch1 = buildOffenderMatch(MatchType.NAME_DOB, CRN, OFFENDER_SEARCH_CRO, OFFENDER_SEARCH_PNC, offenderAliases);
            assertThat(newDefendant.getGroupedOffenderMatches().getMatches()).containsExactly(offenderMatch1);
        }

        @DisplayName("Map a defendant to a new defendant when search response has yielded multiple matches")
        @Test
        void givenMultipleMatches_whenMapNewFromDefendantAndSearchResponse_thenCreateNewDefendantWithListOfMatches() {
            var match1 = Match.builder().offender(OSOffender.builder()
                            .otherIds(OtherIds.builder().crn(CRN).croNumber(OFFENDER_SEARCH_CRO).pncNumber(OFFENDER_SEARCH_PNC).build())
                            .build())
                    .build();
            var match2 = Match.builder().offender(OSOffender.builder()
                            .otherIds(OtherIds.builder().crn("CRN1").build())
                            .offenderAliases(offenderAliases)
                            .build())
                    .build();

            var defendant = HearingMapper.newFromLibraHearing(aLibraHearing)
                    .getDefendants()
                    .get(0);
            var matchResponse = MatchResponse.builder()
                    .matchedBy(OffenderSearchMatchType.PARTIAL_NAME)
                    .matches(List.of(match1, match2))
                    .build();

            var newDefendant = HearingMapper.updateDefendantWithMatches(defendant, matchResponse);

            assertThat(newDefendant).isNotSameAs(defendant);
            assertThat(newDefendant.getCrn()).isNull();
            assertThat(newDefendant.getPnc()).isNull();
            assertThat(newDefendant.getCro()).isNull();
            assertThat(newDefendant.getOffender()).isNull();
            assertThat(newDefendant.getProbationStatus()).isNull();
            assertThat(newDefendant.getBreach()).isNull();
            assertThat(newDefendant.getPreviouslyKnownTerminationDate()).isNull();
            assertThat(newDefendant.getGroupedOffenderMatches().getMatches()).hasSize(2);
            var offenderMatch1 = buildOffenderMatch(MatchType.PARTIAL_NAME, CRN, OFFENDER_SEARCH_CRO, OFFENDER_SEARCH_PNC, null);
            var offenderMatch2 = buildOffenderMatch(MatchType.PARTIAL_NAME, "CRN1", null, null, offenderAliases);
            assertThat(newDefendant.getGroupedOffenderMatches().getMatches()).containsExactlyInAnyOrder(offenderMatch1, offenderMatch2);
        }

        private OffenderMatch buildOffenderMatch(MatchType matchType, String crn, String cro, String pnc, List<OffenderAlias> offenderAliases) {
            return buildOffenderMatch(matchType, crn, cro, pnc, offenderAliases, Mono.empty());
        }
        private OffenderMatch buildOffenderMatch(MatchType matchType, String crn, String cro, String pnc, List<OffenderAlias> offenderAliases, Mono<Double> matchProbability) {
            return OffenderMatch.builder()
                    .matchType(matchType)
                    .confirmed(false)
                    .rejected(false)
                    .matchIdentifiers(MatchIdentifiers.builder().pnc(pnc).cro(cro).crn(crn).aliases(offenderAliases).build())
                    .matchProbability(matchProbability)
                    .build();
        }
    }

    @DisplayName("New from incoming JSON case")
    @Nested
    class NewFromIncomingLibraHearing {

        @DisplayName("Map from a new JSON case (with no block) composed of nulls. Ensures no null pointers.")
        @Test
        void givenJsonCase_whenMapCaseWithNullsThenCreateNewCaseNoOffences_EnsureNoNullPointer() {
            var nullCase = LibraHearing.builder()
                    .courtCode(COURT_CODE)
                    .courtRoom("00")
                    .sessionStartTime(LocalDateTime.of(DATE_OF_HEARING, START_TIME))
                    .caseNo("123")
                    .build();
            final var actual = HearingMapper.newFromLibraHearing(nullCase);
            assertThat(actual).isNotNull();
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
            var aCase = LibraHearing.builder()
                    .caseNo("123")
                    .offences(Arrays.asList(offence2, offence1))
                    .build();

            var courtCase = HearingMapper.newFromLibraHearing(aCase);

            final var firstDefendant = courtCase.getDefendants().get(0);
            assertThat(firstDefendant.getOffences()).hasSize(2);
            var offence = firstDefendant.getOffences().get(0);
            assertThat(offence.getSequenceNumber()).isEqualTo(1);
            assertThat(offence.getAct()).isEqualTo("Contrary to section 2(2) and 8 of the Theft Act 1968.");
            assertThat(offence.getOffenceSummary()).isEqualTo("On 02/02/2022 at Town, stole Article, to the value of £0.02, belonging to Person.");
            assertThat(offence.getOffenceTitle()).isEqualTo("Theft from a person");
        }
    }

    @DisplayName("Merge incoming case to existing Hearing")
    @Nested
    class MergeIncomingToExistingHearing {

        private Hearing existingHearing;
        private Hearing libraCase;
        private Hearing commonPlatformCase;

        @BeforeEach
        void beforeEach() {
            existingHearing = Hearing.builder()
                    .caseNo("12345")
                    .caseId(CASE_ID)
                    .hearingDays(singletonList(HearingDay.builder()
                            .courtCode(COURT_CODE)
                            .courtRoom("01")
                            .listNo("999st")
                            .sessionStartTime(LocalDateTime.of(2020, Month.JANUARY, 3, 9, 10, 0))
                            .build()))

                    .defendants(singletonList(Defendant.builder()
                            .defendantId("9E27A145-E847-4AAB-9FF9-B88912520D14")
                            .breach(Boolean.TRUE)
                            .suspendedSentenceOrder(Boolean.TRUE)
                            .crn("X320741")
                            .pnc("PNC")
                            .probationStatus("CURRENT")
                            .address(null)
                            .name(Name.builder()
                                    .forename1("Pat")
                                    .surname("Garrett")
                                    .build())
                            .type(DefendantType.ORGANISATION)
                            .dateOfBirth(LocalDate.of(1969, Month.JANUARY, 1))
                            .sex("N")
                            .previouslyKnownTerminationDate(LocalDate.of(2001, Month.AUGUST, 26))
                            .offences(singletonList(Offence.builder()
                                    .act("act")
                                    .sequenceNumber(1)
                                    .offenceSummary("summary")
                                    .offenceTitle("title")
                                    .build()))
                            .confirmedOffender(true)
                            .build()))
                    .build();

            libraCase = Hearing.builder()
                    .hearingDays(singletonList(HearingDay.builder()
                            .courtCode(COURT_CODE)
                            .sessionStartTime(LocalDateTime.of(DATE_OF_HEARING, START_TIME))
                            .courtRoom("00")
                            .listNo("1st")
                            .build()))

                    .caseNo("123")
                    .defendants(singletonList(Defendant.builder()
                            .address(Address.builder().line1("line 1").line2("line 2").line3("line 3").postcode("LD1 1AA").build())
                            .dateOfBirth(null)
                            .name(name)
                            .sex("M")
                            .type(DefendantType.PERSON)
                            .offences(singletonList(buildOffenceDomain("NEW Theft from a person", 1)))
                            .build()))
                    .source(LIBRA)
                    .build();

            commonPlatformCase = libraCase
                    .withDefendants(List.of(Defendant.builder()
                                    .defendantId(DEFENDANT_ID)
                                    .address(Address.builder().line1("line 1").line2("line 2").line3("line 3").postcode("LD1 1AA").build())
                                    .dateOfBirth(null)
                                    .name(name)
                                    .sex("M")
                                    .type(DefendantType.PERSON)
                                    .offences(singletonList(buildOffenceDomain("NEW Theft from a person", 1)))
                                    .build(),
                            Defendant.builder()
                                    .defendantId(DEFENDANT_ID_2)
                                    .address(Address.builder().line1("line 1").line2("line 2").line3("line 3").postcode("LD1 1AA").build())
                                    .dateOfBirth(DATE_OF_BIRTH)
                                    .name(name)
                                    .sex("M")
                                    .type(DefendantType.PERSON)
                                    .offences(singletonList(buildOffenceDomain("NEW Theft from a person", 1)))
                                    .build()))
                    .withSource(COMMON_PLATFORM)
                    .withCaseId(CASE_ID)
            ;
        }

        @DisplayName("Merge the case with the existing court case, including offences")
        @Test
        void givenCaseFromJson_whenMergeWithExistingCase_ThenUpdateExistingCase() {

            final var updatedLibraCase = libraCase.withDefendants(singletonList(
                    libraCase.getDefendants().get(0)
                            .withDateOfBirth(null)
            ));

            var courtCase = HearingMapper.merge(updatedLibraCase, existingHearing);
            assertLibraCourtCase(courtCase);

        }

        @DisplayName("Use existing defendantId and caseId if present")
        @Test
        void givenLibraCase_whenMergeWithExistingCasewithIds_ThenUseExistingIds() {

            final var existingCaseId = "82034D44-B709-4227-9CF9-CBFC67F98041";
            final var existingHearingId = "2cfd6e1d-7ee9-47e3-9302-92057e269479";
            final var existingDefendantId = "C09C6A23-0390-41BB-948C-08399BD72720";
            final var existingCourtCase = Hearing.builder()
                    .hearingId(existingHearingId)
                    .caseId(existingCaseId)
                    .defendants(singletonList(Defendant.builder()
                            .defendantId(existingDefendantId)
                            .build()))
                    .build();

            var courtCase = HearingMapper.merge(libraCase, existingCourtCase);

            assertThat(courtCase.getCaseId()).isEqualTo(existingCaseId);
            assertThat(courtCase.getHearingId()).isEqualTo(existingHearingId);
            assertThat(courtCase.getDefendants().get(0).getDefendantId()).isEqualTo(existingDefendantId);

        }

        @DisplayName("Use new libra case hearing Id if existing hearing is empty")
        @Test
        void givenLibraCase_whenExistingHearingIdIsNill_ThenUseNewLibraHearingId() {

            final var existingCaseId = "82034D44-B709-4227-9CF9-CBFC67F98041";
            final var existingDefendantId = "C09C6A23-0390-41BB-948C-08399BD72720";
            final var existingPersonId = "a70fd29a-a9e3-4629-86e0-7817f052cdd1";
            final var existingCourtCase = Hearing.builder()
                    .hearingId(null)
                    .caseId(existingCaseId)
                    .defendants(singletonList(Defendant.builder()
                            .defendantId(existingDefendantId)
                            .personId(existingPersonId)
                            .build()))
                    .build();

            var courtCase = HearingMapper.merge(libraCase, existingCourtCase);

            assertThat(courtCase.getCaseId()).isEqualTo(existingCaseId);
            assertThat(courtCase.getHearingId()).isEqualTo(libraCase.getHearingId());
            assertThat(courtCase.getDefendants().get(0).getDefendantId()).isEqualTo(existingDefendantId);
            assertThat(courtCase.getDefendants().get(0).getPersonId()).isEqualTo(existingPersonId);
        }


        @DisplayName("For Common Platform case use existing caseId and look up defendantId from the existing defendants")
        @Test
        void givenCommonPlatformCase_whenMergeWithExistingCasewithIds_ThenUpdateUsingExistingIds() {

            final var existingCourtCase = Hearing.builder()
                    .caseId(CASE_ID)
                    .defendants(List.of(Defendant.builder()
                                    .defendantId("Ignored defendantId")
                                    .crn("ignored crn")
                                    .build(),
                            Defendant.builder()
                                    .defendantId(DEFENDANT_ID)
                                    .crn("expected crn")
                                    .personId("person id 1")
                                    .build()))
                    .build();

            var courtCase = HearingMapper.merge(commonPlatformCase, existingCourtCase);

            assertThat(courtCase.getCaseId()).isEqualTo(CASE_ID);
            assertThat(courtCase.getDefendants().get(0).getDefendantId()).isEqualTo(DEFENDANT_ID);
            assertThat(courtCase.getDefendants().get(0).getCrn()).isEqualTo("expected crn");
            assertThat(courtCase.getDefendants().get(0).getPersonId()).isEqualTo("person id 1");

            assertThat(courtCase.getCaseId()).isEqualTo(CASE_ID);
            assertThat(courtCase.getDefendants().get(1).getDefendantId()).isEqualTo(DEFENDANT_ID_2);
            assertThat(courtCase.getDefendants().get(1).getCrn()).isEqualTo(null);
            assertThat(courtCase.getDefendants().get(1).getPersonId()).isEqualTo(null);

        }

        @DisplayName("Update from existing case for Libra cases")
        @Test
        void givenLibraCase_whenMergeWithExistingCaseWithoutIds_ThenUseNewIds() {

            final var existingCourtCase = Hearing.builder()
                    .caseId(CASE_ID)
                    .defendants(List.of(Defendant.builder()
                                    .defendantId(DEFENDANT_ID)
                                    .crn("expected crn")
                                    .build(),
                            Defendant.builder()
                                    .defendantId("ignored defendantId")
                                    .crn("ignored crn")
                                    .build()
                    ))
                    .build();

            var courtCase = HearingMapper.merge(libraCase, existingCourtCase);

            assertThat(courtCase.getCaseId()).isEqualTo(CASE_ID);
            assertThat(courtCase.getDefendants().get(0).getDefendantId()).isEqualTo(DEFENDANT_ID);
            assertThat(courtCase.getDefendants().get(0).getCrn()).isEqualTo("expected crn");
        }

        private void assertLibraCourtCase(Hearing hearing) {
            // Fields that stay the same on existing value
            assertThat(hearing.getCourtCode()).isEqualTo(COURT_CODE);
            assertThat(hearing.getCaseNo()).isEqualTo("12345");
            final var firstDefendant = hearing.getDefendants().get(0);
            assertThat(firstDefendant.getProbationStatus()).isEqualTo("CURRENT");
            assertThat(firstDefendant.getBreach()).isTrue();
            assertThat(firstDefendant.getSuspendedSentenceOrder()).isTrue();
            assertThat(firstDefendant.getCrn()).isEqualTo("X320741");
            assertThat(firstDefendant.getPnc()).isEqualTo("PNC");
            assertThat(hearing.getCaseId()).isEqualTo(CASE_ID);
            assertThat(firstDefendant.getDefendantId()).isEqualTo(DEFENDANT_ID);
            // Fields that get overwritten from Libra incoming (even if null)
            assertThat(hearing.getCourtRoom()).isEqualTo("00");
            assertThat(firstDefendant.getAddress().getLine1()).isEqualTo("line 1");
            assertThat(firstDefendant.getAddress().getLine2()).isEqualTo("line 2");
            assertThat(firstDefendant.getAddress().getLine3()).isEqualTo("line 3");
            assertThat(firstDefendant.getAddress().getPostcode()).isEqualTo("LD1 1AA");
            assertThat(firstDefendant.getDateOfBirth()).isNull();
            assertThat(firstDefendant.getName()).isEqualTo(name);
            assertThat(firstDefendant.getType()).isSameAs(DefendantType.PERSON);
            assertThat(firstDefendant.getSex()).isEqualTo("MALE");
            assertThat(hearing.getSessionStartTime()).isEqualTo(SESSION_START_TIME);
            assertThat(firstDefendant.getPreviouslyKnownTerminationDate()).isEqualTo(LocalDate.of(2001, Month.AUGUST, 26));
            assertThat(firstDefendant.getOffences()).hasSize(1);
            assertThat(firstDefendant.getOffences().get(0).getOffenceTitle()).isEqualTo("NEW Theft from a person");
            assertThat(firstDefendant.getOffences().get(0).getSequenceNumber()).isEqualTo(1);
            assertThat(firstDefendant.getConfirmedOffender()).isEqualTo(true);
        }
    }

    @DisplayName("Merge ProbationStatusDetail to existing Hearing")
    @Nested
    class MergeProbationStatusDetailToExistingHearing {

        @DisplayName("Merge the gateway case with the existing court case, including offences")
        @Test
        void whenMergeWithExistingCase_ThenUpdateExistingCase() {

            var existingDefendant = Defendant.builder()
                            .defendantId(DEFENDANT_ID)
                            .breach(Boolean.TRUE)
                            .suspendedSentenceOrder(Boolean.TRUE)
                            .crn(CRN)
                            .pnc(OFFENDER_SEARCH_PNC)
                            .cro(OFFENDER_SEARCH_CRO)
                            .address(null)
                            .name(Name.builder()
                                    .forename1("Pat")
                                    .surname("Garrett")
                                    .build())
                            .type(DefendantType.PERSON)
                            .dateOfBirth(LocalDate.of(1969, Month.JANUARY, 1))
                            .name(Name.builder().forename1("Pat").surname("Garrett").build())
                            .sex("N")
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

            var nextPrevKnownTermDate = existingDefendant.getPreviouslyKnownTerminationDate().plusDays(1);
            var probationStatusDetail = ProbationStatusDetail.builder()
                    .preSentenceActivity(true)
                    .inBreach(Boolean.TRUE)
                    .previouslyKnownTerminationDate(nextPrevKnownTermDate)
                    .status("CURRENT")
                    .awaitingPsr(true)
                    .build();

            var updatedDefendant = HearingMapper.merge(probationStatusDetail, existingDefendant);

            assertThat(updatedDefendant).isNotSameAs(existingDefendant);

            // Fields that are updated
            assertThat(updatedDefendant.getProbationStatus()).isEqualTo("CURRENT");
            assertThat(updatedDefendant.getPreviouslyKnownTerminationDate()).isEqualTo(nextPrevKnownTermDate);
            assertThat(updatedDefendant.getBreach()).isTrue();
            assertThat(updatedDefendant.getPreSentenceActivity()).isTrue();
            assertThat(updatedDefendant.getAwaitingPsr()).isTrue();
            // Fields that stay the same on existing value
            assertThat(updatedDefendant.getDefendantId()).isEqualTo(existingDefendant.getDefendantId());
            assertThat(updatedDefendant.getCrn()).isEqualTo(existingDefendant.getCrn());
            assertThat(updatedDefendant.getCro()).isEqualTo(existingDefendant.getCro());
            assertThat(updatedDefendant.getAddress()).isEqualTo(existingDefendant.getAddress());
            assertThat(updatedDefendant.getDateOfBirth()).isEqualTo(existingDefendant.getDateOfBirth());
            assertThat(updatedDefendant.getName()).isEqualTo(existingDefendant.getName());
            assertThat(updatedDefendant.getSex()).isEqualTo(existingDefendant.getSex());
            assertThat(updatedDefendant.getType()).isSameAs(existingDefendant.getType());
            assertThat(updatedDefendant.getName()).isEqualTo(existingDefendant.getName());
            assertThat(updatedDefendant.getPnc()).isEqualTo(existingDefendant.getPnc());
            assertThat(updatedDefendant.getSuspendedSentenceOrder()).isEqualTo(existingDefendant.getSuspendedSentenceOrder());
            assertThat(updatedDefendant.getOffences()).hasSize(1);
            assertThat(updatedDefendant.getOffences().get(0).getOffenceTitle()).isEqualTo("title");
            assertThat(updatedDefendant.getOffences().get(0).getSequenceNumber()).isEqualTo(1);
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
