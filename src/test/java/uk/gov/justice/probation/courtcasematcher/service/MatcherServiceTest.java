package uk.gov.justice.probation.courtcasematcher.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.type.DefendantType;
import uk.gov.justice.probation.courtcasematcher.model.type.MatchType;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;
import uk.gov.justice.probation.courtcasematcher.model.domain.ProbationStatusDetail;
import uk.gov.justice.probation.courtcasematcher.pact.DomainDataHelper;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.PersonMatchScoreRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchRequest;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OSOffender;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OffenderSearchMatchType;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OtherIds;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personmatchscore.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

@ExtendWith(MockitoExtension.class)
class MatcherServiceTest {
    private static final String CRN = "X123456";
    private static final String PROBATION_STATUS = "CURRENT";
    private static final LocalDate PREVIOUSLY_KNOWN_TERMINATION_DATE = LocalDate.of(2021, 9,29);
    private static final boolean PRE_SENTENCE_ACTIVITY = true;
    private static final boolean BREACH = true;
    private static final boolean AWAITING_PSR = true;

    private static final String PNC = "PNC";
    private static final String PNC_2 = "PNC2";

    private static final LocalDate DEF_DOB = LocalDate.of(2000, 6, 17);
    private static final Name DEF_NAME = Name.builder()
            .forename1("Arthur")
            .surname("MORGAN")
            .build();

    private static final LocalDate DEF_DOB_2 = LocalDate.of(2001, 6, 17);
    private static final Name DEF_NAME_2 = Name.builder()
            .forename1("John")
            .surname("Marston")
            .build();

    private static final Hearing COURT_CASE;

    static {
        final var defendant1 = Defendant.builder()
                .defendantId("id1")
                .name(DEF_NAME)
                .dateOfBirth(DEF_DOB)
                .pnc(PNC)
                .type(DefendantType.PERSON)
                .build();

        final var defendant2 = Defendant.builder()
                .defendantId("id2")
                .name(DEF_NAME_2)
                .dateOfBirth(DEF_DOB_2)
                .pnc(PNC_2)
                .type(DefendantType.PERSON)
                .build();

        COURT_CASE = DomainDataHelper.aHearingBuilderWithAllFields()
                .defendants(List.of(defendant1, defendant2))
                .build();
    }
    private static final Defendant FIRST_DEFENDANT = COURT_CASE.getDefendants().get(0);
    private static final Defendant SECOND_DEFENDANT = COURT_CASE.getDefendants().get(1);

    private final OtherIds otherIds = OtherIds.builder()
            .crn(CRN)
            .croNumber("CRO")
            .pncNumber(PNC)
            .build();
    private final OSOffender offender = OSOffender.builder()
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
    private final MatchResponse singleExactMatch = MatchResponse.builder()
            .matches(singletonList(Match.builder()
                    .offender(offender)
                    .build()))
            .matchedBy(OffenderSearchMatchType.ALL_SUPPLIED)
            .build();
    private final MatchResponse multipleExactMatches = MatchResponse.builder()
            .matches(Arrays.asList(
                    Match.builder()
                            .offender(offender)
                            .build(),
                    Match.builder()
                            .offender(offender)
                            .build()))
            .matchedBy(OffenderSearchMatchType.ALL_SUPPLIED)
            .build();
    private final MatchResponse noMatches = MatchResponse.builder()
            .matchedBy(OffenderSearchMatchType.NOTHING)
            .matches(Collections.emptyList())
            .build();
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

    private final PersonMatchScoreRequest personMatchScoreReq1 = PersonMatchScoreRequest.builder()
      .firstName(PersonMatchScoreParameter.of("Arthur", "Aur"))
      .surname(PersonMatchScoreParameter.of("MORGAN", "Mork"))
      .pnc(PersonMatchScoreParameter.of(PNC, PNC))
      .dateOfBirth(PersonMatchScoreParameter.of("2000-06-17", "2000-06-17"))
      .sourceDataset(PersonMatchScoreParameter.of("COMMON_PLATFORM", "DELIUS"))
      .uniqueId(PersonMatchScoreParameter.of("id1", "id1"))
      .build();
    private final PersonMatchScoreRequest personMatchScoreReq2 = PersonMatchScoreRequest.builder()
      .firstName(PersonMatchScoreParameter.of("John", "Jon"))
      .surname(PersonMatchScoreParameter.of("Marston", "Barston"))
      .dateOfBirth(PersonMatchScoreParameter.of("2001-06-17", "2001-06-17"))
      .pnc(PersonMatchScoreParameter.of(PNC_2, PNC_2))
      .sourceDataset(PersonMatchScoreParameter.of("COMMON_PLATFORM", "DELIUS"))
      .uniqueId(PersonMatchScoreParameter.of("id2", "id2"))
      .build();

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

    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private MatcherService matcherService;

    @BeforeEach
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        Logger logger = (Logger) getLogger(LoggerFactory.getLogger(MatcherService.class).getName());
        logger.addAppender(mockAppender);

        matcherService = new MatcherService(offenderSearchRestClient, personMatchScoreRestClient, matchRequestFactory, telemetryService);
    }

    @Test
    void givenIncomingDefendantDoesNotMatchAnOffender_whenMatchCalled_thenLog() {
        when(matchRequestFactory.buildFrom(FIRST_DEFENDANT)).thenReturn(matchRequest1);
        when(matchRequestFactory.buildFrom(SECOND_DEFENDANT)).thenReturn(matchRequest2);
        when(offenderSearchRestClient.match(matchRequest1)).thenReturn(Mono.just(noMatches));
        when(offenderSearchRestClient.match(matchRequest2)).thenReturn(Mono.just(noMatches));

        var updatedCase = matcherService.matchDefendants(COURT_CASE).take(Duration.ofSeconds(5)).block();

        assertThat(updatedCase.getDefendants().get(0).getGroupedOffenderMatches().getMatches()).isEmpty();
        LoggingEvent loggingEvent = captureLogEvent(0);
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(loggingEvent.getFormattedMessage().trim())
                .contains("Match results for defendantId: id1 - matchedBy: NOTHING, matchCount: 0");
        LoggingEvent loggingEvent2 = captureLogEvent(1);
        assertThat(loggingEvent2.getLevel()).isEqualTo(Level.INFO);
        assertThat(loggingEvent2.getFormattedMessage().trim())
                .contains("Match results for defendantId: id2 - matchedBy: NOTHING, matchCount: 0");
    }

    @Test
    void givenException_whenBuildingMatchRequest_thenLog() {
        final var courtCase = COURT_CASE.withDefendants(singletonList(FIRST_DEFENDANT));
        when(matchRequestFactory.buildFrom(FIRST_DEFENDANT))
                .thenThrow(new IllegalArgumentException("This is the reason"));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> matcherService.matchDefendants(courtCase).block());

        LoggingEvent loggingEvent = captureLogEvent(0);
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.WARN);
        assertThat(loggingEvent.getFormattedMessage().trim())
                .contains("Unable to create MatchRequest for defendantId: id1");
        assertThat(loggingEvent.getThrowableProxy().getClassName()).isEqualTo("java.lang.IllegalArgumentException");
        assertThat(loggingEvent.getThrowableProxy().getMessage()).isEqualTo("This is the reason");
        verify(telemetryService).trackOffenderMatchFailureEvent(FIRST_DEFENDANT, courtCase);
    }

    @Test
    void givenMatchesToMultipleOffenders_whenMatchCalled_thenReturn() {
        when(matchRequestFactory.buildFrom(FIRST_DEFENDANT)).thenReturn(matchRequest1);
        when(matchRequestFactory.buildFrom(SECOND_DEFENDANT)).thenReturn(matchRequest2);
        when(offenderSearchRestClient.match(matchRequest1)).thenReturn(Mono.just(multipleExactMatches));
        when(offenderSearchRestClient.match(matchRequest2)).thenReturn(Mono.just(noMatches));

        when(personMatchScoreRestClient.match(any()))
          .thenReturn(Mono.just(PersonMatchScoreResponse.builder().matchProbability(PersonMatchScoreParameter.of(0.91, null)).build()))
          .thenReturn(Mono.just(PersonMatchScoreResponse.builder().matchProbability(PersonMatchScoreParameter.of(0.81, null)).build()));

        var updatedCase = matcherService.matchDefendants(COURT_CASE).block();

        assertThat(updatedCase).isNotNull();
        var groupedOffenderMatches = updatedCase.getDefendants().get(0).getGroupedOffenderMatches();
        assertThat(groupedOffenderMatches.getMatches()).hasSize(2);
        assertThat(groupedOffenderMatches.getMatches().get(0).getMatchType()).isSameAs(MatchType.NAME_DOB_PNC);
        assertThat(groupedOffenderMatches.getMatches().get(0).getMatchIdentifiers().getCrn()).isSameAs(CRN);
        assertThat(groupedOffenderMatches.getMatches().get(0).getMatchProbability()).isEqualTo(0.91);
        assertThat(groupedOffenderMatches.getMatches().get(1).getMatchType()).isSameAs(MatchType.NAME_DOB_PNC);
        assertThat(groupedOffenderMatches.getMatches().get(1).getMatchIdentifiers().getCrn()).isSameAs(CRN);
        assertThat(groupedOffenderMatches.getMatches().get(1).getMatchProbability()).isEqualTo(0.81);

        verify(telemetryService).trackOffenderMatchEvent(FIRST_DEFENDANT, COURT_CASE, multipleExactMatches);
        verify(telemetryService).trackOffenderMatchEvent(SECOND_DEFENDANT, COURT_CASE, noMatches);

        verify(personMatchScoreRestClient, times(2)).match(AdditionalMatchers.or(
            eq(personMatchScoreReq1), eq(personMatchScoreReq2)));
    }

    @Test
    void givenMatchesToSingleOffender_whenSearchResponse_thenReturnWithProbationStatus() {
        when(matchRequestFactory.buildFrom(FIRST_DEFENDANT)).thenReturn(matchRequest1);
        when(matchRequestFactory.buildFrom(SECOND_DEFENDANT)).thenReturn(matchRequest2);
        when(offenderSearchRestClient.match(matchRequest1)).thenReturn(Mono.just(singleExactMatch));
        when(offenderSearchRestClient.match(matchRequest2)).thenReturn(Mono.just(singleExactMatch));

        when(personMatchScoreRestClient.match(any()))
          .thenReturn(Mono.just(PersonMatchScoreResponse.builder().matchProbability(PersonMatchScoreParameter.of(0.91, null)).build()));

        var updatedCase = matcherService.matchDefendants(COURT_CASE).block();

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
        assertThat(groupedOffenderMatches.getMatches().get(0).getMatchProbability()).isEqualTo(0.91);
    }

    @Test
    void givenZeroMatches_whenSearchResponse_thenReturn() {
        when(matchRequestFactory.buildFrom(COURT_CASE.getDefendants().get(0))).thenReturn(matchRequest1);
        when(matchRequestFactory.buildFrom(COURT_CASE.getDefendants().get(1))).thenReturn(matchRequest2);
        when(offenderSearchRestClient.match(matchRequest1)).thenReturn(Mono.just(noMatches));
        when(offenderSearchRestClient.match(matchRequest2)).thenReturn(Mono.just(noMatches));

        var updatedCase = matcherService.matchDefendants(COURT_CASE).block();

        assertThat(updatedCase).isNotNull();
        assertThat(updatedCase.getDefendants().get(0).getGroupedOffenderMatches().getMatches()).isEmpty();
        assertThat(updatedCase.getDefendants().get(1).getGroupedOffenderMatches().getMatches()).isEmpty();

        verifyNoInteractions(personMatchScoreRestClient);
        verify(telemetryService).trackOffenderMatchEvent(FIRST_DEFENDANT, COURT_CASE, noMatches);
        verify(telemetryService).trackOffenderMatchEvent(SECOND_DEFENDANT, COURT_CASE, noMatches);
    }

    @Test
    void givenDefendantShouldNotMatch_whenSearchResponse_thenDontSearch() {
        final var mockDefendant = mock(Defendant.class);
        when(mockDefendant.shouldMatchToOffender()).thenReturn(false);
        var courtCase = COURT_CASE.withDefendants(singletonList(mockDefendant));
        var updatedCase = matcherService.matchDefendants(courtCase).block();

        assertThat(updatedCase).isEqualTo(courtCase);
        verifyNoMoreInteractions(offenderSearchRestClient, telemetryService);
    }

    private LoggingEvent captureLogEvent(int index) {
        verify(mockAppender, atLeastOnce()).doAppend(captorLoggingEvent.capture());

        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        return events.get(index);
    }
}
