package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsMessageReceiverTest {

    @Mock
    private ExternalDocumentMessageProcessor externalDocumentMessageProcessor;

    @Mock
    private CaseMessageProcessor messageProcessor;

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private MessageParser<ExternalDocumentRequest> parser;

    @Mock
    private AutoCloseable operation;

    private SqsMessageReceiver sqsMessageReceiver;

    @DisplayName("Given a valid XML message then track and process")
    @Test
    void givenValidMessage_whenReceived_ThenTrackAndProcess() throws JsonProcessingException {
        when(telemetryService.withOperation("operationId")).thenReturn(operation);


        sqsMessageReceiver.receiveXml("<xml>test</xml>", "MessageID");


        verify(telemetryService).withOperation("operationId");
        verify(telemetryService).trackSQSMessageEvent("MessageID");
        verify(externalDocumentMessageProcessor).process("<xml>test</xml>", "MessageID");
        verify(operation).close();

    }

    @DisplayName("Given a valid XML message but flag indicating to ignore then do no track or process ")
    @Test
    void givenValidXmlMessageFlagIndicatesIgnore_whenReceived_ThenIgnore() {
        sqsMessageReceiver = new SqsMessageReceiver(externalDocumentMessageProcessor, messageProcessor, telemetryService, "queueName", "queueName", true);

        sqsMessageReceiver.receiveXml("<xml>test</xml>", "MessageID");

        verifyNoMoreInteractions(telemetryService, externalDocumentMessageProcessor);
    }

    @DisplayName("Given a valid JSON message then track and process")
    @Test
    void givenMessage_whenReceived_ThenTrackAndProcess() {
        sqsMessageReceiver = new SqsMessageReceiver(externalDocumentMessageProcessor, messageProcessor, telemetryService, "queueName", "queueName", true);

        sqsMessageReceiver.receive("{}", "MessageID");

        verify(telemetryService).trackSQSMessageEvent("MessageID");
        verify(messageProcessor).process("{}", "MessageID");
    }

    @DisplayName("Given a valid JSON message but flag indicating to ignore then do no track or process ")
    @Test
    void givenValidJsonMessageFlagIndicatesIgnore_whenReceived_ThenIgnore() {
        sqsMessageReceiver = new SqsMessageReceiver(externalDocumentMessageProcessor, messageProcessor, telemetryService, "queueName", "queueName", false);

        sqsMessageReceiver.receive("{}", "MessageID");

        verifyNoMoreInteractions(telemetryService, externalDocumentMessageProcessor);
    }
}
