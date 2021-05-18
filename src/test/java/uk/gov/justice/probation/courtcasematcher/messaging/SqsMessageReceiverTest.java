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

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsMessageReceiverTest {

    @Mock
    private MessageProcessor messageProcessor;

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private EventBus eventBus;

    @Mock
    private AutoCloseable operation;

    @Mock
    private MessageParser<ExternalDocumentRequest> parser;

    @InjectMocks
    private SqsMessageReceiver messageReceiver;

    @DisplayName("Given a valid message then track and process")
    @Test
    void givenValidMessage_whenReceived_ThenTrackAndProcess() throws Exception {
        when(telemetryService.withOperation("operationId")).thenReturn(operation);
        ExternalDocumentRequest externalDocumentRequest = ExternalDocumentRequest.builder().build();
        when(parser.parseMessage("message", ExternalDocumentRequest.class)).thenReturn(externalDocumentRequest);

        messageReceiver.receive("message", "MessageID", "operationId");

        verify(telemetryService).withOperation("operationId");
        verify(messageProcessor).process(externalDocumentRequest, "MessageID");
        verify(telemetryService).trackSQSMessageEvent("MessageID");
        verify(operation).close();
    }

    @DisplayName("Given a valid message then track and process")
    @Test
    void givenInvalidMessage_whenReceived_ThenTrackAndEventFail() throws Exception {
        when(telemetryService.withOperation("operationId")).thenReturn(operation);
        when(parser.parseMessage("message", ExternalDocumentRequest.class)).thenThrow(JsonProcessingException.class);

        try {
            messageReceiver.receive("message", "MessageID", "operationId");
            fail("Expected a RuntimeException");
        }
        catch (RuntimeException ex) {
            verify(telemetryService).withOperation("operationId");
            verify(telemetryService).trackSQSMessageEvent("MessageID");
            verify(eventBus).post(any(CourtCaseFailureEvent.class));
            verify(operation).close();
        }
    }

}
