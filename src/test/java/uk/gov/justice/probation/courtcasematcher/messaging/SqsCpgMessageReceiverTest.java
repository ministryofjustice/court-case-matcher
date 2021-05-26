package uk.gov.justice.probation.courtcasematcher.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsCpgMessageReceiverTest {

    @Mock
    private ExternalDocumentMessageProcessor externalDocumentMessageProcessor;

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private AutoCloseable operation;

    @DisplayName("Given a valid XML message then track and process")
    @Test
    void givenValidMessage_whenReceived_ThenTrackAndProcess() throws Exception {
        var sqsMessageReceiver = new SqsCpgMessageReceiver(externalDocumentMessageProcessor, telemetryService, "queueName");

        when(telemetryService.withOperation("operationId")).thenReturn(operation);

        sqsMessageReceiver.receiveXml("<xml>test</xml>", "MessageID", "operationId");

        verify(telemetryService).withOperation("operationId");
        verify(telemetryService).trackSQSMessageEvent("MessageID");
        verify(externalDocumentMessageProcessor).process("<xml>test</xml>", "MessageID");
        verify(operation).close();
    }

}
