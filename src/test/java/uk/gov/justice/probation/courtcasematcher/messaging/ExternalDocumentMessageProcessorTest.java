package uk.gov.justice.probation.courtcasematcher.messaging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.eventbus.EventBus;
import javax.validation.Validator;
import lombok.Builder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseMatchEvent;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseUpdateEvent;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Info;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.DefendantType.ORGANISATION;
import static uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.DefendantType.PERSON;

@DisplayName("ExternalDocumentMessageProcessor")
@ExtendWith(MockitoExtension.class)
class ExternalDocumentMessageProcessorTest {

    private static final long MATCHER_THREAD_TIMEOUT = 4000;
    private static final String COURT_CODE_CX = "B01CX";
    private static final String COURT_CODE_CY = "B01CY";
    private static final String CRN = "X340741";

    public static String singleCaseXml;
    private static String multiSessionXml;
    private static String multiDayXml;
    private static String multiCourtRoomXml;

    @Mock
    private Validator validator;

    @Mock
    private EventBus eventBus;

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private CourtCaseService courtCaseService;

    @InjectMocks
    private ExternalDocumentMessageProcessor externalDocumentMessageProcessor;

    @BeforeAll
    static void beforeAll() throws IOException {
        final String basePath = "src/test/resources/messages/xml";
        multiSessionXml = Files.readString(Paths.get(basePath +"/external-document-request-multi-session.xml"));
        singleCaseXml = Files.readString(Paths.get(basePath +"/external-document-request-single-case.xml"));
        multiDayXml = Files.readString(Paths.get(basePath +"/external-document-request-multi-day.xml"));
        multiDayXml = Files.readString(Paths.get(basePath +"/external-document-request-multi-day.xml"));
        multiCourtRoomXml = Files.readString(Paths.get(basePath +"/external-document-request-multi-court-room.xml"));
    }

    @BeforeEach
    void beforeEach() {
        var xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        var xmlMapper = new XmlMapper(xmlModule);
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        xmlMapper.registerModule(new JavaTimeModule());
        ReflectionTestUtils.setField(externalDocumentMessageProcessor, "parser", new MessageParser<ExternalDocumentRequest>(xmlMapper, validator));
    }
    @DisplayName("Receive a valid unmatched case for person then post match event to the event bus")
    @Test
    void whenValidMessageReceivedForPerson_ThenMatch() {

        CourtCase courtCase =  CourtCase.builder().defendantType(PERSON).build();
        when(courtCaseService.getCourtCase(any(Case.class))).thenReturn(Mono.just(courtCase));

        externalDocumentMessageProcessor.process(singleCaseXml, "messageId");

        verify(eventBus, timeout(MATCHER_THREAD_TIMEOUT)).post(any(CourtCaseMatchEvent.class));
        verify(telemetryService).trackCourtListEvent(any(Info.class), eq("messageId"));
        verify(telemetryService).trackCourtCaseEvent(any(Case.class), eq("messageId"));
        verifyNoMoreInteractions(eventBus, telemetryService);
    }


    @DisplayName("Receive a valid case for organisation then no match event, post update event to the event bus")
    @Test
    void whenValidMessageReceivedForOrganisation_ThenUpdateEvent() {

        CourtCase courtCase = CourtCase.builder().defendantType(ORGANISATION).build();
        when(courtCaseService.getCourtCase(any(Case.class))).thenReturn(Mono.just(courtCase));

        externalDocumentMessageProcessor.process(singleCaseXml, "messageId");

        verify(eventBus, timeout(MATCHER_THREAD_TIMEOUT)).post(any(CourtCaseUpdateEvent.class));
        verify(telemetryService).trackCourtListEvent(any(Info.class), eq("messageId"));
        verify(telemetryService).trackCourtCaseEvent(any(Case.class), eq("messageId"));
        verifyNoMoreInteractions(eventBus, telemetryService);
    }

    @DisplayName("Receive a valid matched case for person then post update event to the event bus")
    @Test
    void whenValidMessageReceivedForMatchedPerson_ThenNoMatch() {

        CourtCase courtCase =  CourtCase.builder().defendantType(PERSON).crn(CRN).build();
        when(courtCaseService.getCourtCase(any(Case.class))).thenReturn(Mono.just(courtCase));

        externalDocumentMessageProcessor.process(singleCaseXml, "messageId");

        verify(eventBus, timeout(MATCHER_THREAD_TIMEOUT)).post(any(CourtCaseUpdateEvent.class));
        verify(telemetryService).trackCourtListEvent(any(Info.class), eq("messageId"));
        verify(telemetryService).trackCourtCaseEvent(any(Case.class), eq("messageId"));
        verifyNoMoreInteractions(eventBus, telemetryService);
    }

    @DisplayName("Receive multiple valid cases then attempt to match two and update two")
    @Test
    void givenTwoExistingAndOneNew_whenValidMessageReceivedWithMultipleSessions_ThenAttemptMatchOnOneAndUpdateOnTwo() {

        CaseMatcher case1 = CaseMatcher.builder().caseNo("1600032953").build();
        CourtCase courtCase1 = CourtCase.builder().crn(CRN).caseNo("1600032953").defendantType(PERSON).build();
        CaseMatcher case2 = CaseMatcher.builder().caseNo("1600032979").build();
        CourtCase courtCase2 = CourtCase.builder().crn("X123456").caseNo("1600032979").defendantType(PERSON).build();
        CaseMatcher case3 = CaseMatcher.builder().caseNo("1600032952").build();
        CourtCase courtCase3 = CourtCase.builder().caseNo("1600032952").defendantType(PERSON).build();
        CaseMatcher case4 = CaseMatcher.builder().caseNo("1600011111").build();
        CourtCase courtCase4 = CourtCase.builder().isNew(true).caseNo("1600011111").defendantType(ORGANISATION).build();

        when(courtCaseService.getCourtCase(argThat(case1))).thenReturn(Mono.just(courtCase1));
        when(courtCaseService.getCourtCase(argThat(case2))).thenReturn(Mono.just(courtCase2));
        when(courtCaseService.getCourtCase(argThat(case3))).thenReturn(Mono.just(courtCase3));
        when(courtCaseService.getCourtCase(argThat(case4))).thenReturn(Mono.just(courtCase4));

        externalDocumentMessageProcessor.process(multiSessionXml, "messageId");

        // 2 new cases get matcher events. The organisation does not get a matching event despite the fact it is new
        verify(eventBus, timeout(1000)).post(argThat(CourtCaseUpdateEventMatcher.builder().caseNo("1600032953").build()));
        verify(eventBus, timeout(1000)).post(argThat(CourtCaseUpdateEventMatcher.builder().caseNo("1600032979").build()));
        verify(eventBus, timeout(1000)).post(argThat(CourtCaseMatchEventMatcher.builder().caseNo("1600032952").build()));
        verify(eventBus, timeout(1000)).post(argThat(CourtCaseUpdateEventMatcher.builder().caseNo("1600011111").build()));
        verify(telemetryService).trackCourtListEvent(argThat(matchesInfoWith(LocalDate.of(2020, Month.FEBRUARY, 20), COURT_CODE_CX)), eq("messageId"));
        verify(telemetryService).trackCourtListEvent(argThat(matchesInfoWith(LocalDate.of(2020, Month.FEBRUARY, 23), COURT_CODE_CY)), eq("messageId"));
        verify(telemetryService).trackCourtCaseEvent(argThat(case1), eq("messageId"));
        verify(telemetryService).trackCourtCaseEvent(argThat(case2), eq("messageId"));
        verify(telemetryService).trackCourtCaseEvent(argThat(case3), eq("messageId"));
        verify(telemetryService).trackCourtCaseEvent(argThat(case4), eq("messageId"));

        verifyNoMoreInteractions(eventBus, telemetryService);
    }

    @DisplayName("Receive multiple matched valid cases then attempt post as updates.")
    @Test
    void whenValidMessageReceivedWithMultipleDays_ThenAttemptMatch() {
        CourtCase courtCase = mock(CourtCase.class);
        when(courtCase.shouldMatchToOffender()).thenReturn(false);
        when(courtCaseService.getCourtCase(any(Case.class))).thenReturn(Mono.just(courtCase));

        externalDocumentMessageProcessor.process(multiDayXml, "messageId");

        verify(eventBus, timeout(MATCHER_THREAD_TIMEOUT).times(6)).post(any(CourtCaseUpdateEvent.class));

        verify(telemetryService).trackCourtListEvent(argThat(matchesInfoWith(LocalDate.of(2020, Month.JULY, 25))), eq("messageId"));
        verify(telemetryService).trackCourtListEvent(argThat(matchesInfoWith(LocalDate.of(2020, Month.JULY, 28))), eq("messageId"));
        verify(telemetryService, times(6)).trackCourtCaseEvent(any(Case.class), eq("messageId"));
        verifyNoMoreInteractions(eventBus, telemetryService);
    }

    @DisplayName("Receive 2 documents with 2 court rooms but one telemetry event is fired.")
    @Test
    void givenMultipleCourtRooms_whenValidMessageReceived_ThenFireOneTelemetryEvent() {
        CourtCase courtCase = mock(CourtCase.class);
        when(courtCase.shouldMatchToOffender()).thenReturn(false);
        when(courtCaseService.getCourtCase(any(Case.class))).thenReturn(Mono.just(courtCase));

        externalDocumentMessageProcessor.process(multiCourtRoomXml, "messageId");

        verify(eventBus, timeout(MATCHER_THREAD_TIMEOUT).times(2)).post(any(CourtCaseUpdateEvent.class));
        verify(telemetryService).trackCourtListEvent(argThat(matchesInfoWith(LocalDate.of(2020, Month.JULY, 25))), eq("messageId"));
        verify(telemetryService, times(2)).trackCourtCaseEvent(any(Case.class), eq("messageId"));
        verifyNoMoreInteractions(eventBus, telemetryService);
    }

    @DisplayName("A valid message but with 0 cases")
    @Test
    void whenCorrectMessageWithZeroCasesReceived_ThenNoEventsPublished() throws IOException {

        String path = "src/test/resources/messages/xml/external-document-request-empty-sessions.xml";

        externalDocumentMessageProcessor.process(Files.readString(Paths.get(path)), "messageId");

        verify(eventBus, never()).post(any(CourtCaseFailureEvent.class));
    }

    @DisplayName("An existing case based on a person does not match")
    @Test
    void givenExistingPersonCase_whenPostCase_thenDoNotMatch() {

        CourtCase courtCase = CourtCase.builder().crn(CRN).caseNo("1").defendantType(PERSON).build();

        externalDocumentMessageProcessor.postCaseEvent(courtCase);

        verify(eventBus).post(argThat(CourtCaseUpdateEventMatcher.builder().caseNo("1").build()));
        verifyNoMoreInteractions(eventBus);
    }

    @DisplayName("Do nothing and no NPE with null DocumentWrapper")
    @Test
    void givenNullDocumentWrapper_thenNoNullPointer() {
        assertThrows(RuntimeException.class, () -> externalDocumentMessageProcessor.process("", "messageId"));
    }

    @Builder
    public static class CourtCaseUpdateEventMatcher implements ArgumentMatcher<CourtCaseUpdateEvent> {

        private final String caseNo;

        @Override
        public boolean matches(CourtCaseUpdateEvent otherCase) {
            return otherCase != null && caseNo.equals(otherCase.getCourtCase().getCaseNo());
        }
    }

    @Builder
    public static class CourtCaseMatchEventMatcher implements ArgumentMatcher<CourtCaseMatchEvent> {

        private final String caseNo;

        @Override
        public boolean matches(CourtCaseMatchEvent otherCase) {
            return otherCase != null && caseNo.equals(otherCase.getCourtCase().getCaseNo());
        }
    }

    @Builder
    public static class CaseMatcher implements ArgumentMatcher<Case> {

        private final String caseNo;

        @Override
        public boolean matches(Case otherCase) {
            return otherCase != null && caseNo.equals(otherCase.getCaseNo());
        }
    }

    @Builder
    public static class InfoMatcher implements ArgumentMatcher<Info> {

        private final LocalDate dateOfHearing;
        private final String courtCode;

        @Override
        public boolean matches(Info otherInfo) {
            return otherInfo != null
                && dateOfHearing.equals(otherInfo.getDateOfHearing())
                && courtCode.equalsIgnoreCase(otherInfo.getOuCode());
        }
    }

    public static InfoMatcher matchesInfoWith(LocalDate dateOfHearing, String courtCode) {
        return InfoMatcher.builder()
            .courtCode(courtCode)
            .dateOfHearing(dateOfHearing)
            .build();
    }

    private InfoMatcher matchesInfoWith(LocalDate dateOfHearing) {
        return matchesInfoWith(dateOfHearing, COURT_CODE_CX);
    }
}
