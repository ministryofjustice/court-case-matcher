package uk.gov.justice.probation.courtcasematcher.restclient;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.type.DefendantType;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;
import uk.gov.justice.probation.courtcasematcher.model.domain.MatchIdentifiers;
import uk.gov.justice.probation.courtcasematcher.model.type.MatchType;
import uk.gov.justice.probation.courtcasematcher.model.domain.OffenderMatch;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.probation.courtcasematcher.pact.DomainDataHelper.*;
import static uk.gov.justice.probation.courtcasematcher.restclient.LegacyCourtCaseRestClientIntTest.WEB_CLIENT_TIMEOUT_MS;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
@ExtendWith(MockitoExtension.class)
class CourtCaseServiceClientIntTest {
    public static final String HEARING_ID_SERVER_ERROR = "771F1C21-D2CA-4235-8659-5C3C7D7C58B6";
    @Mock
    private Appender<ILoggingEvent> mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;
    @Mock
    private Mono<Hearing> courtCaseMono;
    @MockBean
    private LegacyCourtCaseRestClient legacyClient;

    @Autowired
    private CourtCaseServiceClient client;

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        Logger logger = (Logger) getLogger(LoggerFactory.getLogger(CourtCaseServiceClient.class).getName());
        logger.addAppender(mockAppender);
    }

    @Test
    void whenGetHearingByHearingIdAndCaseId_thenItsSuccessful() {
        final var HEARING_ID = "8bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f";
        final var hearing = client.getHearing(HEARING_ID, CASE_ID).block();

        assertThat(hearing.getCaseId()).isEqualTo(CASE_ID);
        assertThat(hearing.getHearingId()).isEqualTo(HEARING_ID);
        assertThat(hearing.getDefendants().get(0).getDefendantId()).isEqualTo(DEFENDANT_ID);
        assertThat(hearing.getDefendants().get(1).getDefendantId()).isEqualTo(DEFENDANT_ID_2);

        MOCK_SERVER.findAllUnmatchedRequests();
        MOCK_SERVER.verify(
                getRequestedFor(urlEqualTo(String.format("/hearing/%s/case/D517D32D-3C80-41E8-846E-D274DC2B94A5", HEARING_ID)))
        );
    }

    @Test
    void givenNotFound_whenGetHearingById_thenReturnEmpty() {
        final var hearing = client.getHearing("NOT_FOUND").blockOptional();

        assertThat(hearing).isEmpty();

        MOCK_SERVER.findAllUnmatchedRequests();
        MOCK_SERVER.verify(
                getRequestedFor(urlEqualTo(String.format("/hearing/%s", "NOT_FOUND")))
        );
    }

    @Test
    void givenError_whenGetHearingById_thenReturnEmpty() {

        assertThatExceptionOfType(WebClientResponseException.class)
                .isThrownBy(()-> client.getHearing("SERVER_ERROR").block())
                .withMessageContaining("INTERNAL_SERVER_ERROR");

        MOCK_SERVER.findAllUnmatchedRequests();
        MOCK_SERVER.verify(
                getRequestedFor(urlEqualTo(String.format("/hearing/%s", "SERVER_ERROR")))
        );
    }

    @Test
    void whenPutHearing_thenItsSuccessful() {
        final var hearing = aHearingBuilderWithAllFields()
                .build();
        final var voidMono = client.putHearing(hearing);
        assertThat(voidMono.blockOptional()).isEmpty();

        MOCK_SERVER.verify(
                putRequestedFor(urlEqualTo(String.format("/hearing/%s", HEARING_ID)))
        );
        assertThat(MOCK_SERVER.findAllUnmatchedRequests().size()).isEqualTo(0);
    }

    @Test
    void givenNullCaseId_whenPutHearing_thenItsSuccessful() {
        final var hearing = aHearingBuilderWithAllFields()
                .caseId(null)
                .build();
        final var voidMono = client.putHearing(hearing);
        assertThat(voidMono.blockOptional()).isEmpty();

        MOCK_SERVER.verify(
                putRequestedFor(urlMatching("/hearing/[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}"))

        );
    }

    @Test
    void whenRestClientThrows500OnPut_ThenThrow() {
        final var hearing = aHearingBuilderWithAllFields()
                .hearingId(HEARING_ID_SERVER_ERROR)
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("X500")
                        .build()))
                .build();

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> client.putHearing(hearing).block())
                .withMessage("Retries exhausted: 1/1");
    }

    @Test
    void getHearing_delegatesToLegacyClient() {
        when(legacyClient.getHearing("court code", "case no", "list no")).thenReturn(courtCaseMono);

        final var actualMono = client.getHearing("court code", "case no", "list no");

        assertThat(actualMono).isEqualTo(courtCaseMono);
    }

    @Test
    void whenPostDefendantMatches_thenMakeRestCallToCourtCaseService() {

        final List<Defendant> defendants = buildDefendants();

        client.postOffenderMatches(CASE_ID, defendants).block();

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        Assertions.assertThat(events).hasSizeGreaterThanOrEqualTo(2);

        MOCK_SERVER.verify(
            postRequestedFor(urlEqualTo(String.format("/defendant/%s/grouped-offender-matches", DEFENDANT_ID)))
        );

        MOCK_SERVER.verify(
            postRequestedFor(urlEqualTo(String.format("/defendant/%s/grouped-offender-matches", DEFENDANT_ID_2)))
        );
    }

    @Test
    void whenRestClientThrows500OnPostDefendantMatches_ThenRetryAndLogRetryExhaustedError() {

        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> client.postOffenderMatches("X500", buildDefendants("HTTP_500")).block())
            .withMessage("Retries exhausted: 1/1");

        verify(mockAppender, timeout(WEB_CLIENT_TIMEOUT_MS).atLeast(1)).doAppend(captorLoggingEvent.capture());

        final var events = captorLoggingEvent.getAllValues();
        Assertions.assertThat(events).hasSizeGreaterThanOrEqualTo(1);
        String className = events.stream()
            .filter(ev -> ev.getLevel() == Level.ERROR)
            .findFirst()
            .map(event -> event.getThrowableProxy().getClassName())
            .orElse(null);
        Assertions.assertThat(className).contains("reactor.core.Exceptions$RetryExhaustedException");
    }

    @Test
    void whenRestClientCalledWithNullOffenderMatches_ThenDoNotPost() {
        final var defendants = List.of(
                Defendant.builder()
                        .type(DefendantType.PERSON)
                        .defendantId(DEFENDANT_ID)
                        .groupedOffenderMatches(null)
                        .build()
        );

        client.postOffenderMatches(CASE_ID, defendants).block();

        MOCK_SERVER.verify(0,
                postRequestedFor(urlEqualTo(String.format("/defendant/%s/grouped-offender-matches", CASE_ID, DEFENDANT_ID)))
        );
        assertThat(MOCK_SERVER.findAllUnmatchedRequests().size()).isEqualTo(0);
    }

    @Test
    void whenRestClientCalledWithEmptyOffenderMatches_ThenPost() {
        final var defendants = List.of(
                Defendant.builder()
                        .type(DefendantType.PERSON)
                        .defendantId("FE6E58E3-D3F1-4721-AB47-69741940847E")
                        .groupedOffenderMatches(GroupedOffenderMatches.builder()
                                .matches(Collections.emptyList())
                                .build())
                        .build()
        );
        client.postOffenderMatches(CASE_ID, defendants).block();

        MOCK_SERVER.verify(
                postRequestedFor(urlEqualTo(String.format("/defendant/%s/grouped-offender-matches", "FE6E58E3-D3F1-4721-AB47-69741940847E")))
        );
    }

    private List<Defendant> buildDefendants() {
        return buildDefendants(null);
    }

    private List<Defendant> buildDefendants(String defendantIdPrefix) {
        final var matches1 = GroupedOffenderMatches.builder()
                .matches(Collections.singletonList(OffenderMatch.builder()
                        .matchType(MatchType.NAME_DOB)
                        .matchIdentifiers(MatchIdentifiers.builder()
                                .crn("X123456")
                                .cro("E1324/11")
                                .pnc("PNC")
                                .build())
                        .build()))
                .build();

        final var matches2 = GroupedOffenderMatches.builder()
                .matches(Collections.singletonList(OffenderMatch.builder()
                        .matchType(MatchType.NAME_DOB)
                        .matchIdentifiers(MatchIdentifiers.builder()
                                .crn("X123457")
                                .cro("E1324/12")
                                .pnc("PNC2")
                                .build())
                        .build()))
                .build();

        return List.of(
                Defendant.builder()
                        .type(DefendantType.PERSON)
                        .defendantId(Optional.ofNullable(defendantIdPrefix).map(s -> s + DEFENDANT_ID).orElse(DEFENDANT_ID))
                        .groupedOffenderMatches(matches1)
                        .build(),
                Defendant.builder()
                        .type(DefendantType.PERSON)
                        .defendantId(Optional.ofNullable(defendantIdPrefix).map(s -> s + DEFENDANT_ID_2).orElse(DEFENDANT_ID_2))
                        .groupedOffenderMatches(matches2)
                        .build()
        );
    }

}
