package uk.gov.justice.probation.courtcasematcher.messaging;

import javax.validation.constraints.NotEmpty;
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

@Slf4j
@Component
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Profile({"sqs-cpg-messaging", "!test"})
public class SqsCpgMessageReceiver {

    @Autowired
    @Qualifier("xmlMessageProcessor")
    private final MessageProcessor xmlMessageProcessor;

    @Autowired
    private final TelemetryService telemetryService;

    @Value("${aws.sqs.crime_portal_gateway_queue_name}")
    private String queueName;

    @SqsListener(value = "${aws.sqs.crime_portal_gateway_queue_name}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void receiveXml(@NotEmpty String message, @Header("MessageId") String messageId, @Header(value = "operation_Id", required = false) String operationId)
        throws Exception {
        log.info("Received message from SQS queue {} with messageId: {}", queueName, messageId);

        try (final var ignored = telemetryService.withOperation(operationId)) {
            telemetryService.trackCaseMessageReceivedEvent(messageId);
            xmlMessageProcessor.process(message, messageId);
        }
    }

}
