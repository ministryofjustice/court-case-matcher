package uk.gov.justice.probation.courtcasematcher.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.google.common.eventbus.EventBus;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.OffenderSearchMatchType;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

@ExtendWith({MockitoExtension.class})
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class EventListenerTest {

    private final static Name DEFENDANT_NAME = Name.builder().forename1("Nic").surname("CAGE").build();
    private final static LocalDate DEFENDANT_DOB = LocalDate.of(1955, Month.SEPTEMBER, 25);
    private final static String CASE = "123456";
    private final static String COURT_CODE = "B10JQ00";
    private final static CourtCase courtCase = CourtCase.builder()
        .defendantName(DEFENDANT_NAME)
        .defendantDob(DEFENDANT_DOB)
        .courtCode(COURT_CODE)
        .caseNo(CASE)
        .build();

    @Mock
    private MatcherService matcherService;

    @Mock
    private CourtCaseService courtCaseService;

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private EventBus eventBus;

    private EventListener eventListener;

    @BeforeEach
    void beforeEach() {
        Logger logger = (Logger) getLogger(Logger.ROOT_LOGGER_NAME);
        logger.addAppender(mockAppender);

        eventBus = new EventBus();
        eventListener = new EventListener(eventBus, matcherService, courtCaseService, telemetryService);
    }

    @DisplayName("Ensure that successful events are logged and counted")
    @Test
    void testSuccessEvent() {
        CourtCase courtCaseApi = CourtCase.builder().caseNo("123").courtCode("SHF").build();

        eventListener.courtCaseEvent(CourtCaseSuccessEvent.builder().courtCase(courtCaseApi).build());

        verify(mockAppender, atLeast(1)).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        assertThat(events).hasSize(1);
        LoggingEvent loggingEvent = events.get(0);
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(loggingEvent.getFormattedMessage().trim())
            .contains("EventBus success event for posting case 123 for court SHF. Total count of successful messages 1");
        assertThat(eventListener.getSuccessCount()).isEqualTo(1);
    }

    @DisplayName("Ensure that failure events are logged and counted")
    @Test
    void testFailureEvent() {

        eventListener.courtCaseEvent(CourtCaseFailureEvent.builder().failureMessage("Problem").build());

        verify(mockAppender, atLeast(1)).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        assertThat(events).hasSize(1);
        LoggingEvent loggingEvent = events.get(0);
        Assertions.assertAll(
            () -> assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR),
            () -> assertThat(loggingEvent.getFormattedMessage().trim()).contains("Message processing failed. Current error count: 1"),
            () -> assertThat(eventListener.getFailureCount()).isEqualTo(1));
    }

    @DisplayName("Ensure that failure events are logged and counted")
    @Disabled("Issue with capturing logging events")
    @Test
    void testFailureEventWithConstraintViolations() {

        Path path = mock(Path.class);
        ConstraintViolation<String> cv = mock(ConstraintViolation.class);
        when(cv.getMessage()).thenReturn("must not be blank");
        when(cv.getPropertyPath()).thenReturn(path);
        when(path.toString()).thenReturn("blocks[0].cases[0].caseNo");

        eventListener.courtCaseEvent(CourtCaseFailureEvent.builder()
            .failureMessage("Problem")
            .violations(Set.of(cv))
            .build());

        verify(mockAppender, atLeast(1)).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        assertThat(events.size()).isGreaterThanOrEqualTo(1);
        LoggingEvent loggingEvent = events.get(0);
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
        assertThat(loggingEvent.getFormattedMessage().trim())
            .contains("Message processing failed. Current error count: 1");
        assertThat(eventListener.getFailureCount()).isEqualTo(1);
    }

    @DisplayName("Ensure that the EventListener is registered by the EventBus")
    @Test
    void checkEventBusRegistration() {
        eventBus.post(CourtCaseFailureEvent.builder().failureMessage("Problem").build());

        assertThat(eventListener.getFailureCount()).isEqualTo(1);
    }

    @DisplayName("Check the match event when the call to the matcher service returns")
    @Test
    void whenCourtCaseUpdated_thenSave() {
        eventListener.courtCaseUpdateEvent(CourtCaseUpdateEvent.builder().courtCase(courtCase).build());

        verify(courtCaseService).saveCourtCase(courtCase);
    }

    @DisplayName("Check the match event when the call to the matcher service returns")
    @Test
    void givenSearch_whenCourtCaseMatched_thenSave() {
        SearchResponse searchResponse = SearchResponse.builder().build();
        when(matcherService.getSearchResponse(DEFENDANT_NAME, DEFENDANT_DOB, COURT_CODE, CASE)).thenReturn(Mono.just(searchResponse));

        eventBus.post(CourtCaseMatchEvent.builder().courtCase(courtCase).build());

        verify(matcherService).getSearchResponse(DEFENDANT_NAME, DEFENDANT_DOB, COURT_CODE, CASE);
        verify(courtCaseService).createCase(courtCase, searchResponse);
        verify(telemetryService).trackOffenderMatchEvent(COURT_CODE, CASE, searchResponse);
    }

    @DisplayName("Check the match event when the call to the matcher service returns an empty response")
    @Test
    void givenSearchWhichFails_whenCourtCaseMatched_thenSave() {
        when(matcherService.getSearchResponse(DEFENDANT_NAME, DEFENDANT_DOB, COURT_CODE, CASE)).thenReturn(Mono.empty());

        eventListener.courtCaseMatchEvent(CourtCaseMatchEvent.builder().courtCase(courtCase).build());

        SearchResponse searchResponse = SearchResponse.builder()
            .matches(Collections.emptyList())
            .matchedBy(OffenderSearchMatchType.NOTHING)
            .build();
        verify(matcherService).getSearchResponse(DEFENDANT_NAME, DEFENDANT_DOB, COURT_CODE, CASE);
        verify(courtCaseService).createCase(courtCase, searchResponse);
        verify(telemetryService).trackOffenderMatchEvent(COURT_CODE, CASE, searchResponse);
    }

    @DisplayName("Check the match event when the call to the matcher service returns an empty response")
    @Test
    void whenOffenderSearchFails_thenLogError() {

        String messageBody = "{\n"
            + "    \"firstName\": \"David\",\n"
            + "    \"surname\": \"JONES\",\n"
            + "    \"dateOfBirth\": \"1990-01-01\"\n"
            + "}";

        eventListener.offenderSearchFailureEvent(OffenderSearchFailureEvent.builder()
                                                                            .requestJson(messageBody)
                                                                            .failureMessage("ERROR")
                                                                            .build());

        verify(mockAppender, atLeast(1)).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        assertThat(events.size()).isGreaterThanOrEqualTo(1);
        LoggingEvent loggingEvent = events.get(0);
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
        assertThat(loggingEvent.getFormattedMessage().trim())
            .contains(messageBody);
    }

}
