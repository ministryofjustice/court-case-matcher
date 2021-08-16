package uk.gov.justice.probation.courtcasematcher.restclient;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.google.common.eventbus.EventBus;
import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseSuccessEvent;
import uk.gov.justice.probation.courtcasematcher.model.domain.Address;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.model.domain.MatchIdentifiers;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;
import uk.gov.justice.probation.courtcasematcher.model.domain.OffenderMatch;
import uk.gov.justice.probation.courtcasematcher.model.gateway.Name;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchType;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClientTest.createException;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
public class CourtCaseRestClientIntTest {

    private static final String COURT_CODE = "B10JQ";
    private static final String CASE_NO = "12345";
    private static final String NEW_CASE_NO = "1600032981";
    private static final int WEB_CLIENT_TIMEOUT_MS = 10000;

    private static final GroupedOffenderMatches matches = GroupedOffenderMatches.builder()
        .matches(Collections.singletonList(OffenderMatch.builder()
            .matchType(MatchType.NAME_DOB)
            .matchIdentifiers(MatchIdentifiers.builder()
                .crn("X123456")
                .cro("E1324/11")
                .pnc("PNC")
                .build())
            .build()))
        .build();

    private static final CourtCase A_CASE = CourtCase.builder()
        .caseId("1246257")
        .caseNo(CASE_NO)
        .courtCode(COURT_CODE)
        .groupedOffenderMatches(matches)
        .build();

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    @MockBean
    private EventBus eventBus;

    @Autowired
    private CourtCaseRestClient restClient;

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        Logger logger = (Logger) getLogger(LoggerFactory.getLogger(CourtCaseRestClient.class).getName());
        logger.addAppender(mockAppender);
    }

    @Test
    void whenGetCourtCase_thenMakeRestCallToCourtCaseService() {

        LocalDateTime startTime = LocalDateTime.of(2020, Month.JANUARY, 13, 9, 0, 0);
        Address address = Address.builder()
            .line1("27")
            .line2("Elm Place")
            .line3("Bangor")
            .postcode("ad21 5dr")
            .build();

        Offence offenceApi = Offence.builder()
            .offenceSummary("On 01/01/2016 at Town, stole Article, to the value of £100.00, belonging to Person.")
            .offenceTitle("Theft from a shop")
            .act("Contrary to section 1(1) and 7 of the Theft Act 1968.")
            .build();

        CourtCase expected = CourtCase.builder()
            .caseId("1246257")
            .crn("X320741")
            .pnc("D/1234560BC")
            .listNo("2nd")
            .courtCode("B10JQ")
            .courtRoom("1")
            .sessionStartTime(startTime)
            .probationStatus("Current")
            .probationStatusActual("CURRENT")
            .breach(Boolean.TRUE)
            .suspendedSentenceOrder(Boolean.FALSE)
            .caseNo(CASE_NO)
            .defendantAddress(address)
            .defendantDob(LocalDate.of(1977, Month.DECEMBER, 11))
            .name(Name.builder().title("Mr")
                .forename1("Dylan")
                .forename2("Adam")
                .surname("ARMSTRONG")
                .build())
            .defendantType(DefendantType.PERSON)
            .defendantSex("M")
            .nationality1("British")
            .nationality2("Czech")
            .offences(Collections.singletonList(offenceApi))
            .build();

        Optional<CourtCase> optional = restClient.getCourtCase(COURT_CODE, "123456").blockOptional();

        assertThat(optional.get()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenUnknownCaseNo_whenGetCourtCase_thenReturnEmptyOptional() {

        Optional<CourtCase> optional = restClient.getCourtCase(COURT_CODE, NEW_CASE_NO).blockOptional();

        assertThat(optional.isPresent()).isFalse();
    }

    @Test
    void whenPutCourtCase_thenMakeRestCallToCourtCaseService() {

        restClient.putCourtCase(COURT_CODE, CASE_NO, A_CASE).block();

        verify(eventBus, timeout(WEB_CLIENT_TIMEOUT_MS)).post(any(CourtCaseSuccessEvent.class));

        MOCK_SERVER.verify(
                putRequestedFor(urlEqualTo(String.format("/court/%s/case/%s", COURT_CODE, CASE_NO)))
        );
    }

    @Test
    void givenUnknownCourt_whenPutCourtCase_thenNoRetryFailureEvent() {

        var unknownCourtCode = "XXX";
        var courtCaseApi = CourtCase.builder()
            .caseNo("12345")
            .courtCode(unknownCourtCode)
            .build();

        assertThatExceptionOfType(WebClientResponseException.class)
            .isThrownBy(() -> restClient.putCourtCase(unknownCourtCode, CASE_NO, courtCaseApi).block())
            .withMessage("404 Not Found from PUT http://localhost:8090/court/XXX/case/12345");

        var notFoundException = createException(HttpStatus.NOT_FOUND).getClass();
        var failureEventMatcher
            = FailureEventMatcher.builder().throwableClass(notFoundException).build();
        verify(eventBus, timeout(WEB_CLIENT_TIMEOUT_MS)).post(argThat(failureEventMatcher));
        MOCK_SERVER.verify(1,
                putRequestedFor(urlEqualTo("/court/XXX/case/12345"))
        );
    }

    @Test
    void whenRestClientThrows500OnPut_ThenThrow() {
        final var aCase = CourtCase.builder()
                .caseId("1246257")
                .caseNo(CASE_NO)
                .courtCode("X500")
                .groupedOffenderMatches(matches)
                .build();

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> restClient.putCourtCase("X500", CASE_NO, aCase).block())
                .withMessage("Retries exhausted: 1/1");

        var retryExhaustedException = Exceptions.retryExhausted("Message", new IllegalArgumentException()).getClass();
        var failureEventMatcher= FailureEventMatcher.builder().throwableClass(retryExhaustedException).build();
        verify(eventBus, timeout(WEB_CLIENT_TIMEOUT_MS)).post(argThat(failureEventMatcher));
    }

    @Test
    void whenPostOffenderMatches_thenMakeRestCallToCourtCaseService() {

        restClient.postMatches(COURT_CODE, "666666", matches).block();

        verify(mockAppender, timeout(WEB_CLIENT_TIMEOUT_MS)).doAppend(captorLoggingEvent.capture());

        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        assertThat(events).hasSizeGreaterThanOrEqualTo(1);
        MOCK_SERVER.verify(
                postRequestedFor(urlEqualTo(String.format("/court/%s/case/%s/grouped-offender-matches", COURT_CODE, "666666")))
        );
    }

    @Test
    void givenUnknownCourt_whenPostOffenderMatches_thenNoRetryAndLogNotFoundError() {

        GroupedOffenderMatches matches = GroupedOffenderMatches.builder()
            .matches(Collections.singletonList(OffenderMatch.builder()
                .matchType(MatchType.NAME_DOB)
                .matchIdentifiers(MatchIdentifiers.builder()
                    .crn("X99999")
                    .cro("E1324/11")
                    .pnc("PNC")
                    .build())
                .build()))
            .build();

        assertThatExceptionOfType(WebClientResponseException.class)
            .isThrownBy(() -> restClient.postMatches("XXX", CASE_NO, matches).block())
            .withMessage("404 Not Found from POST http://localhost:8090/court/XXX/case/12345/grouped-offender-matches");

        verify(mockAppender, timeout(WEB_CLIENT_TIMEOUT_MS)).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        assertThat(events).hasSizeGreaterThanOrEqualTo(1);
        String className = events.stream()
            .filter(ev -> ev.getLevel() == Level.ERROR)
            .findFirst()
            .map(event -> event.getThrowableProxy().getClassName())
            .orElse(null);
        assertThat(className).contains("WebClientResponseException$NotFound");
    }

    @Test
    void whenRestClientThrows500OnPostMatches_ThenRetryAndLogRetryExhaustedError() {

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> restClient.postMatches("X500", CASE_NO, matches).block())
                .withMessage("Retries exhausted: 1/1");

        verify(mockAppender, timeout(WEB_CLIENT_TIMEOUT_MS).atLeast(1)).doAppend(captorLoggingEvent.capture());

        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        assertThat(events).hasSizeGreaterThanOrEqualTo(1);
        String className = events.stream()
            .filter(ev -> ev.getLevel() == Level.ERROR)
            .findFirst()
            .map(event -> event.getThrowableProxy().getClassName())
            .orElse(null);
        assertThat(className).contains("reactor.core.Exceptions$RetryExhaustedException");
    }

    @Test
    void whenRestClientCalledWithNull_ThenFailureEvent() {

        restClient.postMatches(COURT_CODE, CASE_NO, null).block();

        verify(mockAppender, never()).doAppend(captorLoggingEvent.capture());
    }

    @Builder
    public static class FailureEventMatcher implements ArgumentMatcher<CourtCaseFailureEvent> {

        private final Class throwableClass;

        @Override
        public boolean matches(CourtCaseFailureEvent argument) {
            return throwableClass.equals(argument.getThrowable().getClass());
        }
    }
}
