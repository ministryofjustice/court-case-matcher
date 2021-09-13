package uk.gov.justice.probation.courtcasematcher.messaging;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsMessageReceiverTest {
    private static final String MESSAGE_ID = "MessageID";
    private static String singleCaseJson;

    @Mock
    private CourtCaseProcessor caseProcessor;
    @Mock
    private TelemetryService telemetryService;
    @Mock
    private AutoCloseable operation;
    @Mock
    private CourtCaseExtractor caseExtractor;

    private CourtCase libraCourtCase = CourtCase.builder()
            .source(DataSource.LIBRA)
            .build();
    private SqsMessageReceiver sqsMessageReceiver;

    @BeforeAll
    static void beforeAll() throws IOException {
        final String basePath = "src/test/resources/messages/libra";
        singleCaseJson = Files.readString(Paths.get(basePath +"/case-sns-metadata.json"));
    }

    @BeforeEach
    public void setUp() {
        sqsMessageReceiver = new SqsMessageReceiver(caseProcessor, telemetryService, "queueName", caseExtractor);
    }

    @DisplayName("Given a valid Libra JSON message then track and process")
    @Test
    void givenLibraMessage_whenReceived_ThenTrackAndProcess() throws Exception {
        when(telemetryService.withOperation("operationId")).thenReturn(operation);
        when(caseExtractor.extractCourtCase(singleCaseJson, MESSAGE_ID)).thenReturn(libraCourtCase);

        sqsMessageReceiver.receive(singleCaseJson, MESSAGE_ID, "operationId");

        verify(telemetryService).withOperation("operationId");
        verify(telemetryService).trackCaseMessageReceivedEvent(MESSAGE_ID);
        verify(caseProcessor).process(libraCourtCase, MESSAGE_ID);
        verify(operation).close();
    }

    @DisplayName("Given a valid Common Platform JSON message then track and process")
    @Test
    void givenCommonPlatformMessage_whenReceived_ThenDontProcess() throws Exception {
        when(telemetryService.withOperation("operationId")).thenReturn(operation);
        when(caseExtractor.extractCourtCase(singleCaseJson, MESSAGE_ID)).thenReturn(CourtCase.builder()
                .source(DataSource.COMMON_PLATFORM)
                .build());

        sqsMessageReceiver.receive(singleCaseJson, MESSAGE_ID, "operationId");

        verify(telemetryService).withOperation("operationId");
        verify(telemetryService).trackCaseMessageReceivedEvent(MESSAGE_ID);
        verify(caseProcessor, never()).process(libraCourtCase, MESSAGE_ID);
        verify(operation).close();
    }

    @Test
    void givenExceptionThrown_whenExtractCase_thenThrow() {
        when(telemetryService.withOperation("operationId")).thenReturn(operation);
        final var runtimeException = new RuntimeException("Bang");
        when(caseExtractor.extractCourtCase(singleCaseJson, MESSAGE_ID)).thenThrow(runtimeException);

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> sqsMessageReceiver.receive(singleCaseJson, MESSAGE_ID, "operationId"))
                .isEqualTo(runtimeException);
    }

    @Test
    void givenExceptionThrown_whenProcessCase_thenThrow() {
        when(telemetryService.withOperation("operationId")).thenReturn(operation);
        final var runtimeException = new RuntimeException("Bang");
        when(caseExtractor.extractCourtCase(singleCaseJson, MESSAGE_ID)).thenReturn(libraCourtCase);
        doThrow(runtimeException).when(caseProcessor).process(libraCourtCase, MESSAGE_ID);

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> sqsMessageReceiver.receive(singleCaseJson, MESSAGE_ID, "operationId"))
                .isEqualTo(runtimeException);
    }

}
