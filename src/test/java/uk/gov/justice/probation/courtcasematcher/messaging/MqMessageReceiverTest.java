package uk.gov.justice.probation.courtcasematcher.messaging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.eventbus.EventBus;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryEventType;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("MqMessageReceiver")
@ExtendWith(MockitoExtension.class)
class MqMessageReceiverTest {

    private static String multiSessionXml;
    private static String singleCaseXml;

    @Mock
    private ExternalDocumentMessageProcessor messageProcessor;

    @Mock
    private Validator validator;

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private EventBus eventBus;

    private MqMessageReceiver messageReceiver;

    @BeforeAll
    static void beforeAll() throws IOException {

        String multipleSessionPath = "src/test/resources/messages/gateway-message-multi-session.xml";
        multiSessionXml = Files.readString(Paths.get(multipleSessionPath));

        String path = "src/test/resources/messages/gateway-message-single-case.xml";
        singleCaseXml = Files.readString(Paths.get(path));
    }

    @BeforeEach
    void beforeEach() {
        var xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        var objectMapper = new XmlMapper(xmlModule);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        messageReceiver = new MqMessageReceiver(messageProcessor, telemetryService, new MessageParser<>(objectMapper, validator));
    }

    @DisplayName("Given valid message then process")
    @Test
    void whenMessageReceived_ThenProcess() {

        messageReceiver.receive(multiSessionXml);

        verify(telemetryService).trackEvent(TelemetryEventType.COURT_LIST_MESSAGE_RECEIVED);
        verify(messageProcessor).process(any(ExternalDocumentRequest.class), isNull());
    }

    @DisplayName("An XML message which is invalid")
    @Test
    void whenInvalidXmlMessageReceived_NothingPublished() {

        try {
            messageReceiver.receive("<someOtherXml>Not the message you are looking for</someOtherXml>");
            fail("Expected a RuntimeException");
        }
        catch (RuntimeException ex) {
            verify(telemetryService).trackEvent(TelemetryEventType.COURT_LIST_MESSAGE_RECEIVED);
        }
    }

    @DisplayName("An XML message which is grammatically valid but fails validation")
    @Test
    void whenInvalidMessageReceived_NothingPublished() {
        @SuppressWarnings("unchecked") ConstraintViolation<MessageType> cv = mock(ConstraintViolation.class);
        when(validator.validate(any(MessageType.class))).thenReturn(Set.of(cv));

        try {
            messageReceiver.receive(singleCaseXml);
            fail("Expected a RuntimeException");
        }
        catch (RuntimeException ex) {
            verify(telemetryService).trackEvent(TelemetryEventType.COURT_LIST_MESSAGE_RECEIVED);
        }

    }
}
