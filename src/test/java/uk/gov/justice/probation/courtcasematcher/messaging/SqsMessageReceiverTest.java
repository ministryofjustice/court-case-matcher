package uk.gov.justice.probation.courtcasematcher.messaging;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsMessageReceiverTest {
    private static final String MESSAGE_ID = "MessageID";
    private static String singleCaseJson;

    @Mock
    private HearingProcessor caseProcessor;
    @Mock
    private TelemetryService telemetryService;
    @Mock
    private HearingExtractor caseExtractor;

    private final List<Hearing> libraHearing = List.of(Hearing.builder()
            .source(DataSource.LIBRA)
            .build());

    private final List<Hearing> commonPlatformHearing = List.of(Hearing.builder()
            .source(DataSource.COMMON_PLATFORM)
            .defendants(List.of(Defendant.builder().crn("12345").build()))
            .build());

    private final List<Hearing> invalidCommonPlatformHearing = List.of(Hearing.builder()
        .source(DataSource.COMMON_PLATFORM)
        .build());

    private SqsMessageReceiver sqsMessageReceiver;

    @BeforeAll
    static void beforeAll() throws IOException {
        final String basePath = "src/test/resources/messages/libra";
        singleCaseJson = Files.readString(Paths.get(basePath +"/case-sns-metadata.json"));
    }

    @BeforeEach
    public void setUp() {
        sqsMessageReceiver = new SqsMessageReceiver(caseProcessor, telemetryService,  caseExtractor);
    }

    @DisplayName("Given a valid Libra JSON message then track and process")
    @Test
    void givenLibraMessage_whenReceived_ThenTrackAndProcess() throws Exception {
        when(caseExtractor.extractHearings(singleCaseJson, MESSAGE_ID)).thenReturn(libraHearing);

        sqsMessageReceiver.receive(singleCaseJson, MESSAGE_ID);

        verify(telemetryService).trackHearingMessageReceivedEvent(MESSAGE_ID);
        verify(caseProcessor).process(libraHearing.getFirst(), MESSAGE_ID);
        verify(caseProcessor).process(libraHearing.getFirst(), MESSAGE_ID);
    }

    @DisplayName("Given a valid Common Platform JSON message then track and process")
    @Test
    void givenCommonPlatformMessage_whenReceived_ThenProcess() throws Exception {
        when(caseExtractor.extractHearings(singleCaseJson, MESSAGE_ID)).thenReturn(commonPlatformHearing);

        sqsMessageReceiver.receive(singleCaseJson, MESSAGE_ID);


        verify(telemetryService).trackHearingMessageReceivedEvent(MESSAGE_ID);
        verify(caseProcessor).process(commonPlatformHearing.getFirst(), MESSAGE_ID);
        verify(caseProcessor).process(commonPlatformHearing.getFirst(), MESSAGE_ID);
    }

    @DisplayName("Given an invalid Common Platform JSON message then do not process message")
    @Test
    void givenInvalidCommonPlatformMessage_whenReceived_ThenDoNotProcess() throws Exception {
        when(caseExtractor.extractHearings(singleCaseJson, MESSAGE_ID)).thenReturn(invalidCommonPlatformHearing);

        sqsMessageReceiver.receive(singleCaseJson, MESSAGE_ID);

        verify(telemetryService).trackHearingMessageReceivedEvent(MESSAGE_ID);
        verifyNoInteractions(caseProcessor); //Common Platform hearing with no defendants should not be processed

    }

    @Test
    void givenExceptionThrown_whenExtractCase_thenThrow() {
        final var runtimeException = new RuntimeException("Bang");
        when(caseExtractor.extractHearings(singleCaseJson, MESSAGE_ID)).thenThrow(runtimeException);

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> sqsMessageReceiver.receive(singleCaseJson, MESSAGE_ID))
                .isEqualTo(runtimeException);
    }

    @Test
    void givenExceptionThrown_whenProcessCase_thenThrow() {
        final var runtimeException = new RuntimeException("Bang");
        when(caseExtractor.extractHearings(singleCaseJson, MESSAGE_ID)).thenReturn(libraHearing);
        doThrow(runtimeException).when(caseProcessor).process(libraHearing.getFirst(), MESSAGE_ID);

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> sqsMessageReceiver.receive(singleCaseJson, MESSAGE_ID))
                .isEqualTo(runtimeException);
    }

}
