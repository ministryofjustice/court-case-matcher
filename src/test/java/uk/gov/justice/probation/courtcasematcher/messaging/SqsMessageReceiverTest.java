package uk.gov.justice.probation.courtcasematcher.messaging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsMessageReceiverTest {

    private static String singleCaseJson;
    @Mock
    private CaseMessageProcessor messageProcessor;

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private AutoCloseable operation;

    @BeforeAll
    static void beforeAll() throws IOException {
        final String basePath = "src/test/resources/messages/json";
        singleCaseJson = Files.readString(Paths.get(basePath +"/case-sns-metadata.json"));
    }

    @DisplayName("Given a valid JSON message then track and process")
    @Test
    void givenMessage_whenReceived_ThenTrackAndProcess() throws Exception {
        var sqsMessageReceiver = new SqsMessageReceiver(messageProcessor, telemetryService, "queueName");
        when(telemetryService.withOperation("operationId")).thenReturn(operation);

        sqsMessageReceiver.receive(singleCaseJson, "MessageID", "operationId");

        verify(telemetryService).withOperation("operationId");
        verify(telemetryService).trackCaseMessageReceivedEvent("MessageID");
        verify(messageProcessor).process(contains("\"Type\" : \"Notification\""), eq("MessageID"));
        verify(operation).close();
    }

}
