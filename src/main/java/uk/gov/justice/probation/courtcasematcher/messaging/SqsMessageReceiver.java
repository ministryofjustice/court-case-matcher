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
            @Header(value = "id") String messageId){
        log.info("Received JSON message from SQS queue with messageId: {}. ", messageId);

        telemetryService.trackHearingMessageReceivedEvent(messageId);
        final var hearings = hearingExtractor.extractHearings(message, messageId);

        hearings.forEach(hearing -> {
            if (hearing.isValidHearingForProcessing()) {
                hearingProcessor.process(hearing, messageId);
            }
        });

    }
}
