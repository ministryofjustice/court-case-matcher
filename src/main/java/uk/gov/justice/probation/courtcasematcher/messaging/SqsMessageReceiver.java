package uk.gov.justice.probation.courtcasematcher.messaging;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import javax.validation.constraints.NotEmpty;

@Slf4j
@Component
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Profile("!test")
public class SqsMessageReceiver {

    @Autowired
    @Qualifier("caseMessageProcessor")
    private final CaseMessageProcessor messageProcessor;

    @Autowired
    private final TelemetryService telemetryService;

    @Value("${aws.sqs.court_case_matcher_queue_name}")
    private String queueName;

    @SqsListener(value = "${aws.sqs.court_case_matcher_queue_name}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void receive(
            @NotEmpty String message,
            @Header("MessageId") String messageId,
            @Header(value = "operation_Id", required = false) String operationId)
            throws Exception {
        log.info("Received JSON message from SQS queue {} with messageId: {}. ", queueName, messageId);
        try (final var ignored = telemetryService.withOperation(operationId)) {
            telemetryService.trackCaseMessageReceivedEvent(messageId);
            messageProcessor.process(message, messageId);
        }
    }
}
