package uk.gov.justice.probation.courtcasematcher.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.eventbus.EventBus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.application.CaseMapperReference;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;

@DisplayName("Message Processor")
@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    private static final String COURT_CODE = "SHF";
    public static final String CASE_NO = "1600032952";

    private static final CaseMapperReference caseMapperReference = new CaseMapperReference();
    private static String singleCaseXml;
    private static String multipleSessionXml;

    @Mock
    private EventBus eventBus;

    @Mock
    private MatcherService matcherService;

    @Mock
    private CourtCaseService courtCaseService;

    @Mock
    private Validator validator;

    private MessageProcessor messageProcessor;

    @Captor
    private ArgumentCaptor<Case> captor;

    @Captor
    private ArgumentCaptor<List<Case>> captorCases;

    @BeforeAll
    static void beforeAll() throws IOException {

        String multipleSessionPath = "src/test/resources/messages/gateway-message-full.xml";
        multipleSessionXml = Files.readString(Paths.get(multipleSessionPath));

        String path = "src/test/resources/messages/gateway-message-single-case.xml";
        singleCaseXml = Files.readString(Paths.get(path));
        caseMapperReference.setDefaultProbationStatus("No record");
        caseMapperReference.setCourtNameToCodes(Map.of("SheffieldMagistratesCourt", COURT_CODE));
    }

    @BeforeEach
    void beforeEach() {
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        GatewayMessageParser parser = new GatewayMessageParser(new XmlMapper(xmlModule), validator);
        messageProcessor = new MessageProcessor(parser, eventBus, matcherService, courtCaseService);
    }

    @DisplayName("Receive a valid case then attempt to match")
    @Test
    void whenValidMessageReceived_ThenAttemptMatch() {

        messageProcessor.process(singleCaseXml);

        verify(matcherService).match(captor.capture());
        assertThat(captor.getValue().getCaseNo()).isEqualTo(CASE_NO);
        assertThat(captor.getValue().getBlock().getSession().getCourtCode()).isEqualTo(COURT_CODE);
    }

    @DisplayName("Receive multiple valid cases then attempt to match")
    @Test
    void whenValidMessageReceivedWithMultipleSessions_ThenAttemptMatch() {

        messageProcessor.process(multipleSessionXml);

        InOrder inOrder = inOrder(matcherService, courtCaseService);
        inOrder.verify(matcherService, timeout(500).times(18)).match(any(Case.class));
        inOrder.verify(courtCaseService, timeout(2000)).purgeAbsent(eq(COURT_CODE), captorCases.capture());

        assertThat(captorCases.getValue().size()).isEqualTo(18);
    }

    @DisplayName("An XML message which is invalid")
    @Test
    void whenInvalidXmlMessageReceived_NothingPublished() {

        messageProcessor.process("<someOtherXml>Not the message you are looking for</someOtherXml>");

        verify(eventBus).post(any(CourtCaseFailureEvent.class));
    }

    @DisplayName("An XML message which is grammatically valid but fails validation")
    @Test
    void whenInvalidMessageReceived_NothingPublished() {
        @SuppressWarnings("unchecked") ConstraintViolation<MessageType> cv = mock(ConstraintViolation.class);
        when(validator.validate(any(MessageType.class))).thenReturn(Set.of(cv));

        messageProcessor.process(singleCaseXml);

        verify(eventBus).post(any(CourtCaseFailureEvent.class));
    }

    @DisplayName("A valid message but with 0 cases")
    @Test
    void whenCorrectMessageWithZeroCasesReceived_ThenNoEventsPublished() throws IOException {

        String path = "src/test/resources/messages/gateway-message-empty-sessions.xml";

        messageProcessor.process(Files.readString(Paths.get(path)));

        verify(eventBus).post(any(CourtCaseFailureEvent.class));
    }


}
