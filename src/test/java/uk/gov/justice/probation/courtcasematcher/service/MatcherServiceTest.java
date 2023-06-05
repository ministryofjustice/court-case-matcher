package uk.gov.justice.probation.courtcasematcher.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.application.FeatureFlags;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;
import uk.gov.justice.probation.courtcasematcher.model.domain.ProbationStatusDetail;
import uk.gov.justice.probation.courtcasematcher.model.type.DefendantType;
import uk.gov.justice.probation.courtcasematcher.model.type.MatchType;
import uk.gov.justice.probation.courtcasematcher.pact.DomainDataHelper;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.PersonMatchScoreRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.PersonRecordServiceClient;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.*;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personmatchscore.PersonMatchScoreParameter;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personmatchscore.PersonMatchScoreRequest;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personmatchscore.PersonMatchScoreResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personrecordservice.Person;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personrecordservice.PersonSearchRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.slf4j.LoggerFactory.getLogger;

@ExtendWith(MockitoExtension.class)
class MatcherServiceTest {
    private static final String CRN = "X123456";
    private static final String PROBATION_STATUS = "CURRENT";
    private static final LocalDate PREVIOUSLY_KNOWN_TERMINATION_DATE = LocalDate.of(2021, 9, 29);
    private static final boolean PRE_SENTENCE_ACTIVITY = true;
    private static final boolean BREACH = true;
    private static final boolean AWAITING_PSR = true;
    private static final String PNC = "PNC";
    private static final String PNC_2 = "PNC2";
    private static final LocalDate DEF_DOB = LocalDate.of(2000, 6, 17);
    private final Name firstDefendantName = Name.builder()
            .forename1("Arthur")
            .surname("MORGAN")
            .build();
    private static final LocalDate DEF_DOB_2 = LocalDate.of(2001, 6, 17);
    private final Name secondDefendantName = Name.builder()
            .forename1("John")
            .surname("Marston")
            .build();
    private Defendant defendant1;
    private Defendant defendant2;
    private OtherIds otherIds;
    private OSOffender offender;
    private MatchResponse singleExactMatch;
    private MatchResponse multipleExactMatches;
    private MatchResponse noMatches;


    private final MatchRequest matchRequest1 = MatchRequest.builder()
            .pncNumber(PNC)
            .firstName("Arthur")
            .surname("MORGAN")
            .dateOfBirth("2000-06-17")
            .build();
    private final MatchRequest matchRequest2 = MatchRequest.builder()
            .pncNumber(PNC_2)
            .firstName("John")
            .surname("Marston")
            .dateOfBirth("2001-06-17")
            .build();

    private PersonMatchScoreRequest personMatchScoreReq1;

    private PersonMatchScoreRequest personMatchScoreReq2;

    private Hearing hearing;

    @Mock
    private OffenderSearchRestClient offenderSearchRestClient;

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Mock
    private MatchRequest.Factory matchRequestFactory;
    @Mock
    private TelemetryService telemetryService;

    @Mock
    private PersonMatchScoreRestClient personMatchScoreRestClient;

    @Mock
    private PersonRecordServiceClient personRecordServiceClient;

    @Mock
    private FeatureFlags featureFlags;

    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    @InjectMocks
    private MatcherService matcherService;

    @BeforeEach
    public void setUp() {
        Logger logger = (Logger) getLogger(LoggerFactory.getLogger(MatcherService.class).getName());
        logger.addAppender(mockAppender);
        personMatchScoreReq1 = createFirstPersonMatchScoreRequest();
        personMatchScoreReq2 = createSecondPersonMatchScoreRequest();
        defendant1 = createFirstDefendant();
        defendant2 = createSecondDefendant();
        otherIds = createOtherIds();
        offender = createOSOffender();
        hearing = createHearing();
        singleExactMatch = buildSingleMatchResponse();
        multipleExactMatches = buildMultipleMatchResponse();
        noMatches = buildNoMatchesResponse();

        lenient().when(personRecordServiceClient.createPerson(any(Person.class))).thenReturn(Mono.just(Person.builder().build()));
    }


    @Test
    void givenIncomingDefendantDoesNotMatchAnOffender_whenMatchCalled_thenLog() {
        when(matchRequestFactory.buildFrom(defendant1)).thenReturn(matchRequest1);
        when(matchRequestFactory.buildFrom(defendant2)).thenReturn(matchRequest2);
        when(offenderSearchRestClient.match(matchRequest1)).thenReturn(Mono.just(noMatches));
        when(offenderSearchRestClient.match(matchRequest2)).thenReturn(Mono.just(noMatches));
        when(featureFlags.getFlag("save_person_id_to_court_case_service")).thenReturn(true);

        when(personRecordServiceClient.search(any(PersonSearchRequest.class)))
                .thenReturn(Mono.just(Arrays.asList(Person.builder().personId(UUID.randomUUID()).build(), Person.builder().personId(UUID.randomUUID()).build())));

        var updatedCase = matcherService.matchDefendants(hearing).take(Duration.ofSeconds(5)).block();

        assertThat(updatedCase.getDefendants().get(0).getGroupedOffenderMatches().getMatches()).isEmpty();

        LoggingEvent loggingEvent = captureLogEvent(0);
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(loggingEvent.getFormattedMessage().trim())
                .contains("Successfully created person in Person Record service");
        LoggingEvent loggingEvent1 = captureLogEvent(1);
        assertThat(loggingEvent1.getLevel()).isEqualTo(Level.INFO);
        assertThat(loggingEvent1.getFormattedMessage().trim())
                .contains("Match results for defendantId: id1 - matchedBy: NOTHING, matchCount: 0");
        LoggingEvent loggingEvent2 = captureLogEvent(2);
        assertThat(loggingEvent2.getLevel()).isEqualTo(Level.INFO);
        assertThat(loggingEvent2.getFormattedMessage().trim())
                .contains("Successfully created person in Person Record service");
        LoggingEvent loggingEvent3 = captureLogEvent(3);
        assertThat(loggingEvent3.getLevel()).isEqualTo(Level.INFO);
        assertThat(loggingEvent3.getFormattedMessage().trim())
                .contains("Match results for defendantId: id2 - matchedBy: NOTHING, matchCount: 0");
    }

    @Test
    void givenException_whenBuildingMatchRequest_thenLog() {
        final var courtCase = hearing.withDefendants(singletonList(defendant1));
        when(matchRequestFactory.buildFrom(defendant1))
                .thenThrow(new IllegalArgumentException("This is the reason"));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> matcherService.matchDefendants(courtCase).block());

        LoggingEvent loggingEvent = captureLogEvent(1);
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.WARN);
        assertThat(loggingEvent.getFormattedMessage().trim())
                .contains("Unable to create MatchRequest for defendantId: id1");
        assertThat(loggingEvent.getThrowableProxy().getClassName()).isEqualTo("java.lang.IllegalArgumentException");
        assertThat(loggingEvent.getThrowableProxy().getMessage()).isEqualTo("This is the reason");
        verify(telemetryService).trackOffenderMatchFailureEvent(defendant1, courtCase);
    }

    @Test
    void givenMatchesToMultipleOffenders_whenMatchCalled_thenReturn() {
        when(matchRequestFactory.buildFrom(defendant1)).thenReturn(matchRequest1);
        when(matchRequestFactory.buildFrom(defendant2)).thenReturn(matchRequest2);
        when(offenderSearchRestClient.match(matchRequest1)).thenReturn(Mono.just(multipleExactMatches));
        when(offenderSearchRestClient.match(matchRequest2)).thenReturn(Mono.just(noMatches));

        when(personMatchScoreRestClient.match(any()))
                .thenReturn(Mono.just(PersonMatchScoreResponse.builder().matchProbability(PersonMatchScoreParameter.of(0.91, null)).build()))
                .thenReturn(Mono.just(PersonMatchScoreResponse.builder().matchProbability(PersonMatchScoreParameter.of(0.81, null)).build()));

        var updatedCase = matcherService.matchDefendants(hearing).block();

        assertThat(updatedCase).isNotNull();
        var groupedOffenderMatches = updatedCase.getDefendants().get(0).getGroupedOffenderMatches();
        assertThat(groupedOffenderMatches.getMatches()).hasSize(2);
        assertThat(groupedOffenderMatches.getMatches().get(0).getMatchType()).isSameAs(MatchType.NAME_DOB_PNC);
        assertThat(groupedOffenderMatches.getMatches().get(0).getMatchIdentifiers().getCrn()).isSameAs(CRN);
        assertThat(groupedOffenderMatches.getMatches().get(0).getMatchProbability().block()).isEqualTo(0.91);
        assertThat(groupedOffenderMatches.getMatches().get(1).getMatchType()).isSameAs(MatchType.NAME_DOB_PNC);
        assertThat(groupedOffenderMatches.getMatches().get(1).getMatchIdentifiers().getCrn()).isSameAs(CRN);
        assertThat(groupedOffenderMatches.getMatches().get(1).getMatchProbability().block()).isEqualTo(0.81);

        verify(telemetryService).trackOffenderMatchEvent(defendant1, hearing, multipleExactMatches);
        verify(telemetryService).trackOffenderMatchEvent(defendant2, hearing, noMatches);

        verify(personMatchScoreRestClient, times(2)).match(AdditionalMatchers.or(
                eq(personMatchScoreReq1), eq(personMatchScoreReq2)));
    }

    @Test
    void shouldAllowMatchingOfDefendantsWithNullDateOfBirth() {
        // Given
        defendant1.setDateOfBirth(null);
        defendant2.setDateOfBirth(null);
        matchRequest2.setDateOfBirth(null);

        when(matchRequestFactory.buildFrom(defendant1)).thenReturn(matchRequest1);
        when(matchRequestFactory.buildFrom(defendant2)).thenReturn(matchRequest2);
        when(offenderSearchRestClient.match(matchRequest1)).thenReturn(Mono.just(multipleExactMatches));
        when(offenderSearchRestClient.match(matchRequest2)).thenReturn(Mono.just(noMatches));

        when(personMatchScoreRestClient.match(any()))
                .thenReturn(Mono.just(PersonMatchScoreResponse.builder().matchProbability(PersonMatchScoreParameter.of(0.91, null)).build()))
                .thenReturn(Mono.just(PersonMatchScoreResponse.builder().matchProbability(PersonMatchScoreParameter.of(0.81, null)).build()));

        // When
        matcherService.matchDefendants(hearing).block();

        // Then
        personMatchScoreReq1.setDateOfBirth(PersonMatchScoreParameter.of("2000-06-17", null));
        personMatchScoreReq2.setDateOfBirth(PersonMatchScoreParameter.of(null, null));
        verify(personMatchScoreRestClient, times(2)).match(AdditionalMatchers.or(
                eq(personMatchScoreReq1), eq(personMatchScoreReq2)));
    }

    @Test
    void givenMatchesToSingleOffender_whenSearchResponse_thenReturnWithProbationStatus() {
        when(matchRequestFactory.buildFrom(defendant1)).thenReturn(matchRequest1);
        when(matchRequestFactory.buildFrom(defendant2)).thenReturn(matchRequest2);
        when(offenderSearchRestClient.match(matchRequest1)).thenReturn(Mono.just(singleExactMatch));
        when(offenderSearchRestClient.match(matchRequest2)).thenReturn(Mono.just(singleExactMatch));

        when(personMatchScoreRestClient.match(any()))
          .thenReturn(Mono.just(PersonMatchScoreResponse.builder().matchProbability(PersonMatchScoreParameter.of(0.91, null)).build()));

        var updatedCase = matcherService.matchDefendants(hearing).block();

        assertThat(updatedCase).isNotNull();
        final var defendant = updatedCase.getDefendants().get(0);
        assertThat(defendant.getProbationStatus()).isEqualTo(PROBATION_STATUS);
        assertThat(defendant.getPreviouslyKnownTerminationDate()).isEqualTo(PREVIOUSLY_KNOWN_TERMINATION_DATE);
        assertThat(defendant.getBreach()).isEqualTo(BREACH);
        assertThat(defendant.getPreSentenceActivity()).isEqualTo(PRE_SENTENCE_ACTIVITY);
        assertThat(defendant.getAwaitingPsr()).isEqualTo(AWAITING_PSR);

        var groupedOffenderMatches = defendant.getGroupedOffenderMatches();
        assertThat(groupedOffenderMatches.getMatches()).hasSize(1);
        assertThat(groupedOffenderMatches.getMatches().get(0).getMatchType()).isSameAs(MatchType.NAME_DOB_PNC);
        assertThat(groupedOffenderMatches.getMatches().get(0).getMatchIdentifiers().getPnc()).isSameAs(PNC);
        assertThat(groupedOffenderMatches.getMatches().get(0).getMatchProbability().block()).isEqualTo(0.91);
    }

    @Test
    void givenMatchesToSingleOffender_whenPersonMatchScoreThrowsError_thenIgnoreAndPostMatches() {
        when(matchRequestFactory.buildFrom(defendant1)).thenReturn(matchRequest1);
        when(matchRequestFactory.buildFrom(defendant2)).thenReturn(matchRequest2);
        when(offenderSearchRestClient.match(matchRequest1)).thenReturn(Mono.just(singleExactMatch));
        when(offenderSearchRestClient.match(matchRequest2)).thenReturn(Mono.just(singleExactMatch));

        when(personMatchScoreRestClient.match(any()))
          .thenReturn(Mono.error(new RuntimeException("Call to person match score API failed")));

        var updatedCase = matcherService.matchDefendants(hearing).block();

        assertThat(updatedCase).isNotNull();
        final var defendant = updatedCase.getDefendants().get(0);
        assertThat(defendant.getProbationStatus()).isEqualTo(PROBATION_STATUS);
        assertThat(defendant.getPreviouslyKnownTerminationDate()).isEqualTo(PREVIOUSLY_KNOWN_TERMINATION_DATE);
        assertThat(defendant.getBreach()).isEqualTo(BREACH);
        assertThat(defendant.getPreSentenceActivity()).isEqualTo(PRE_SENTENCE_ACTIVITY);
        assertThat(defendant.getAwaitingPsr()).isEqualTo(AWAITING_PSR);

        var groupedOffenderMatches = defendant.getGroupedOffenderMatches();
        assertThat(groupedOffenderMatches.getMatches()).hasSize(1);
        assertThat(groupedOffenderMatches.getMatches().get(0).getMatchType()).isSameAs(MatchType.NAME_DOB_PNC);
        assertThat(groupedOffenderMatches.getMatches().get(0).getMatchIdentifiers().getPnc()).isSameAs(PNC);
        assertThat(groupedOffenderMatches.getMatches().get(0).getMatchProbability().block()).isNull();
    }

    @Test
    void givenZeroMatches_whenSearchResponse_thenReturn() {
        when(matchRequestFactory.buildFrom(hearing.getDefendants().get(0))).thenReturn(matchRequest1);
        when(matchRequestFactory.buildFrom(hearing.getDefendants().get(1))).thenReturn(matchRequest2);
        when(offenderSearchRestClient.match(matchRequest1)).thenReturn(Mono.just(noMatches));
        when(offenderSearchRestClient.match(matchRequest2)).thenReturn(Mono.just(noMatches));

        var updatedCase = matcherService.matchDefendants(hearing).block();

        assertThat(updatedCase).isNotNull();
        assertThat(updatedCase.getDefendants().get(0).getGroupedOffenderMatches().getMatches()).isEmpty();
        assertThat(updatedCase.getDefendants().get(1).getGroupedOffenderMatches().getMatches()).isEmpty();

        verifyNoInteractions(personMatchScoreRestClient);
        verify(telemetryService).trackOffenderMatchEvent(defendant1, hearing, noMatches);
        verify(telemetryService).trackOffenderMatchEvent(defendant2, hearing, noMatches);
    }

    @Test
    void shouldUpdateEachDefendantWithPersonIdFromCreatedPersonRecordWhenFlagIsSet() {
        // Given
        when(featureFlags.getFlag("save_person_id_to_court_case_service")).thenReturn(true);

        UUID personId = UUID.randomUUID();
        Person person = Person.builder()
                .personId(personId)
                .build();

        when(personRecordServiceClient.createPerson(any(Person.class))).thenReturn(Mono.just(person));


        when(personRecordServiceClient.search(any(PersonSearchRequest.class)))
                .thenReturn(Mono.just(Collections.emptyList()));

        when(matchRequestFactory.buildFrom(defendant1)).thenReturn(matchRequest1);
        when(matchRequestFactory.buildFrom(defendant2)).thenReturn(matchRequest2);
        when(offenderSearchRestClient.match(matchRequest1)).thenReturn(Mono.just(noMatches));
        when(offenderSearchRestClient.match(matchRequest2)).thenReturn(Mono.just(noMatches));

        // When
        var updatedCase = matcherService.matchDefendants(hearing).block();

        // Then
        verify(personRecordServiceClient, times(2)).createPerson(any(Person.class));
        verify(telemetryService, times(2)).trackPersonRecordCreatedEvent(any(Defendant.class), any(Hearing.class));
        assertThat(updatedCase.getDefendants())
                .allMatch(defendant -> defendant.getPersonId().equals(personId.toString()));
    }

    @Test
    void shouldNOTUpdateEachDefendantWithPersonIdFromCreatedPersonRecordWhenFlagIsNOTSet() {
        // Given
        when(featureFlags.getFlag("save_person_id_to_court_case_service")).thenReturn(false);

        when(matchRequestFactory.buildFrom(defendant1)).thenReturn(matchRequest1);
        when(matchRequestFactory.buildFrom(defendant2)).thenReturn(matchRequest2);
        when(offenderSearchRestClient.match(matchRequest1)).thenReturn(Mono.just(noMatches));
        when(offenderSearchRestClient.match(matchRequest2)).thenReturn(Mono.just(noMatches));

        // When
        var updatedCase = matcherService.matchDefendants(hearing).block();

        // Then
        assertThat(updatedCase.getDefendants())
                .allMatch(defendant -> defendant.getPersonId() == null);
    }

    @Test
    void givenDefendantShouldNotMatch_whenSearchResponse_thenDontSearch() {
        // Given
        final var mockDefendant = mock(Defendant.class);
        when(mockDefendant.shouldMatchToOffender()).thenReturn(false);
        var courtCase = hearing.withDefendants(singletonList(mockDefendant));

        // When
        var updatedCase = matcherService.matchDefendants(courtCase).block();

        // Then
        assertThat(updatedCase).isEqualTo(courtCase);
        verifyNoMoreInteractions(offenderSearchRestClient);
    }

    @Test
    void shouldSetDefendantWithPersonIdWhenFlagIsSetAndPersonRecordSearchReturnWithExactMatch() {
        // Given
        when(featureFlags.getFlag("save_person_id_to_court_case_service")).thenReturn(true);

        UUID personId = UUID.randomUUID();
        Person person = Person.builder()
                .personId(personId)
                .build();


        when(personRecordServiceClient.search(any(PersonSearchRequest.class)))
                .thenReturn(Mono.just(Collections.singletonList(person)));

        when(personMatchScoreRestClient.match(any()))
                .thenReturn(Mono.just(PersonMatchScoreResponse.builder().matchProbability(PersonMatchScoreParameter.of(0.91, null)).build()));

        when(matchRequestFactory.buildFrom(defendant1)).thenReturn(matchRequest1);
        when(matchRequestFactory.buildFrom(defendant2)).thenReturn(matchRequest2);
        when(offenderSearchRestClient.match(matchRequest1)).thenReturn(Mono.just(singleExactMatch));
        when(offenderSearchRestClient.match(matchRequest2)).thenReturn(Mono.just(singleExactMatch));

        // When
        var updatedCase = matcherService.matchDefendants(hearing).block();

        // Then
        verify(personRecordServiceClient, times(2)).search(any(PersonSearchRequest.class));
        assertThat(updatedCase.getDefendants())
                .allMatch(defendant -> defendant.getPersonId().equals(personId.toString()));
    }

    @Test
    void shouldNotSetDefendantWithPersonIdWhenFlagIsNotSet() {
        // Given
        when(featureFlags.getFlag("save_person_id_to_court_case_service")).thenReturn(false);

        UUID personId = UUID.randomUUID();
        Person person = Person.builder()
                .personId(personId)
                .build();


        when(personMatchScoreRestClient.match(any()))
                .thenReturn(Mono.just(PersonMatchScoreResponse.builder().matchProbability(PersonMatchScoreParameter.of(0.91, null)).build()));

        when(matchRequestFactory.buildFrom(defendant1)).thenReturn(matchRequest1);
        when(matchRequestFactory.buildFrom(defendant2)).thenReturn(matchRequest2);
        when(offenderSearchRestClient.match(matchRequest1)).thenReturn(Mono.just(singleExactMatch));
        when(offenderSearchRestClient.match(matchRequest2)).thenReturn(Mono.just(singleExactMatch));

        // When
        var updatedCase = matcherService.matchDefendants(hearing).block();

        // Then
        verify(personRecordServiceClient, times(0)).createPerson(any(Person.class));
        verify(personRecordServiceClient, times(0)).search(any(PersonSearchRequest.class));

    }

    @Test
    void shouldCreatePersonRecordAndSetDefendantWithPersonIdWhenFlagIsSetAndPersonRecordSearchSearchReturnEmpty() {
        // Given
        when(featureFlags.getFlag("save_person_id_to_court_case_service")).thenReturn(true);

        UUID personId = UUID.randomUUID();
        Person person = Person.builder()
                .personId(personId)
                .build();

        when(personRecordServiceClient.createPerson(any(Person.class))).thenReturn(Mono.just(person));

        when(personRecordServiceClient.search(any(PersonSearchRequest.class)))
                .thenReturn(Mono.just(Collections.emptyList()));


        when(personMatchScoreRestClient.match(any()))
                .thenReturn(Mono.just(PersonMatchScoreResponse.builder().matchProbability(PersonMatchScoreParameter.of(0.91, null)).build()));

        when(matchRequestFactory.buildFrom(defendant1)).thenReturn(matchRequest1);
        when(matchRequestFactory.buildFrom(defendant2)).thenReturn(matchRequest2);
        when(offenderSearchRestClient.match(matchRequest1)).thenReturn(Mono.just(multipleExactMatches));
        when(offenderSearchRestClient.match(matchRequest2)).thenReturn(Mono.just(multipleExactMatches));

        // When
        var updatedCase = matcherService.matchDefendants(hearing).block();

        // Then
        verify(personRecordServiceClient, times(2)).createPerson(any(Person.class));

    }

    @Test
    void shouldCreatePersonRecordAndSetDefendantWithPersonIdWhenFlagIsSetAndPersonRecordSearchSearchReturnMoreResults() {
        // Given
        when(featureFlags.getFlag("save_person_id_to_court_case_service")).thenReturn(true);

        UUID personId = UUID.randomUUID();
        Person person1 = Person.builder()
                .personId(personId)
                .build();

        Person person2 = Person.builder()
                .personId(UUID.randomUUID())
                .build();

        when(personRecordServiceClient.createPerson(any(Person.class))).thenReturn(Mono.just(person1));

        when(personRecordServiceClient.search(any(PersonSearchRequest.class)))
                .thenReturn(Mono.just(Arrays.asList(person1,person2)));


        when(personMatchScoreRestClient.match(any()))
                .thenReturn(Mono.just(PersonMatchScoreResponse.builder().matchProbability(PersonMatchScoreParameter.of(0.91, null)).build()));

        when(matchRequestFactory.buildFrom(defendant1)).thenReturn(matchRequest1);
        when(matchRequestFactory.buildFrom(defendant2)).thenReturn(matchRequest2);
        when(offenderSearchRestClient.match(matchRequest1)).thenReturn(Mono.just(multipleExactMatches));
        when(offenderSearchRestClient.match(matchRequest2)).thenReturn(Mono.just(multipleExactMatches));

        // When
        var updatedCase = matcherService.matchDefendants(hearing).block();

        // Then
        verify(personRecordServiceClient, times(2)).createPerson(any(Person.class));

    }


    private LoggingEvent captureLogEvent(int index) {
        verify(mockAppender, atLeastOnce()).doAppend(captorLoggingEvent.capture());

        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        return events.get(index);
    }

    private Defendant createSecondDefendant() {
        return Defendant.builder()
                .defendantId("id2")
                .name(secondDefendantName)
                .dateOfBirth(DEF_DOB_2)
                .pnc(PNC_2)
                .type(DefendantType.PERSON)
                .build();
    }

    private Defendant createFirstDefendant() {
        return Defendant.builder()
                .defendantId("id1")
                .name(firstDefendantName)
                .dateOfBirth(DEF_DOB)
                .pnc(PNC)
                .type(DefendantType.PERSON)
                .build();
    }

    private Hearing createHearing() {
        return DomainDataHelper.aHearingBuilderWithAllFields()
                .defendants(List.of(defendant1, defendant2))
                .build();
    }

    private OSOffender createOSOffender() {
        return OSOffender.builder()
                .firstName("Aur")
                .surname("Mork")
                .dateOfBirth("1975-01-01")
                .otherIds(otherIds)
                .probationStatus(ProbationStatusDetail.builder()
                        .status(PROBATION_STATUS)
                        .previouslyKnownTerminationDate(PREVIOUSLY_KNOWN_TERMINATION_DATE)
                        .inBreach(BREACH)
                        .preSentenceActivity(PRE_SENTENCE_ACTIVITY)
                        .awaitingPsr(AWAITING_PSR)
                        .build())
                .build();
    }

    private OtherIds createOtherIds() {
        return OtherIds.builder()
                .crn(CRN)
                .croNumber("CRO")
                .pncNumber(PNC)
                .build();
    }

    private PersonMatchScoreRequest createFirstPersonMatchScoreRequest() {
        return PersonMatchScoreRequest.builder()
                .firstName(PersonMatchScoreParameter.of("Arthur", "Aur"))
                .surname(PersonMatchScoreParameter.of("MORGAN", "Mork"))
                .pnc(PersonMatchScoreParameter.of(PNC, PNC))
                .dateOfBirth(PersonMatchScoreParameter.of("2000-06-17", "2000-06-17"))
                .sourceDataset(PersonMatchScoreParameter.of("COMMON_PLATFORM", "DELIUS"))
                .uniqueId(PersonMatchScoreParameter.of("id1", "id1"))
                .build();
    }

    private PersonMatchScoreRequest createSecondPersonMatchScoreRequest() {
        return PersonMatchScoreRequest.builder()
                .firstName(PersonMatchScoreParameter.of("John", "Jon"))
                .surname(PersonMatchScoreParameter.of("Marston", "Barston"))
                .dateOfBirth(PersonMatchScoreParameter.of("2001-06-17", "2001-06-17"))
                .pnc(PersonMatchScoreParameter.of(PNC_2, PNC_2))
                .sourceDataset(PersonMatchScoreParameter.of("COMMON_PLATFORM", "DELIUS"))
                .uniqueId(PersonMatchScoreParameter.of("id2", "id2"))
                .build();
    }

    private MatchResponse buildSingleMatchResponse() {
        return MatchResponse.builder()
                .matches(singletonList(Match.builder()
                        .offender(offender)
                        .build()))
                .matchedBy(OffenderSearchMatchType.ALL_SUPPLIED)
                .build();
    }

    private MatchResponse buildMultipleMatchResponse() {
        return MatchResponse.builder()
                .matches(Arrays.asList(
                        Match.builder()
                                .offender(offender)
                                .build(),
                        Match.builder()
                                .offender(offender)
                                .build()))
                .matchedBy(OffenderSearchMatchType.ALL_SUPPLIED)
                .build();
    }

    private MatchResponse buildNoMatchesResponse() {
        return MatchResponse.builder()
                .matchedBy(OffenderSearchMatchType.NOTHING)
                .matches(Collections.emptyList())
                .build();
    }

}
