package uk.gov.justice.probation.courtcasematcher.messaging;

import io.awspring.cloud.sqs.annotation.SqsListener;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

@Slf4j
@Component
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class SqsMessageReceiver {

    @Autowired
    private final HearingProcessor hearingProcessor;

    @Autowired
    private final TelemetryService telemetryService;

    @Autowired
    @NonNull
    private final HearingExtractor hearingExtractor;

    @SqsListener(value = "courtcasematcherqueue",  factory = "hmppsQueueContainerFactoryProxy")
    public void receive(
            @NotEmpty String message,
            @Header(value = "MessageId", required = false) String messageId,
            @Header(value = "operation_Id", required = false) String operationId)
            throws Exception {
        log.info("Received JSON message from SQS queue with messageId: {}. ", messageId);

        try (final var ignored = telemetryService.withOperation(operationId)) {
            telemetryService.trackHearingMessageReceivedEvent(messageId);
            final var hearing = hearingExtractor.extractHearing(message, messageId);

            if (hearing.isValidHearingForProcessing()) {
                hearingProcessor.process(hearing, messageId);
            }
        }
    }
}
