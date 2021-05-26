package uk.gov.justice.probation.courtcasematcher.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import static org.mockito.Mockito.verify;
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

    @DisplayName("Given a valid JSON message then track and process")
    @Test
    void givenMessage_whenReceived_ThenTrackAndProcess() throws Exception {
        var sqsMessageReceiver = new SqsMessageReceiver(messageProcessor, telemetryService, "queueName");
        when(telemetryService.withOperation("operationId")).thenReturn(operation);

        sqsMessageReceiver.receive("{}", "MessageID", "operationId");

        verify(telemetryService).withOperation("operationId");
        verify(telemetryService).trackSQSMessageEvent("MessageID");
        verify(messageProcessor).process("{}", "MessageID");
        verify(operation).close();
    }

}
