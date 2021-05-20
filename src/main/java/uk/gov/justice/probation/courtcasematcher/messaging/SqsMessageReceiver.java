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
@Profile("sqs-messaging")
public class SqsMessageReceiver {

    @Autowired
    @Qualifier("xmlMessageProcessor")
    private final MessageProcessor xmlMessageProcessor;

    @Autowired
    @Qualifier("caseMessageProcessor")
    private final MessageProcessor messageProcessor;

    @Autowired
    private final TelemetryService telemetryService;

    @Value("${aws.sqs.crime_portal_gateway_queue_name}")
    private String cpgQueueName;

    @Value("${aws.sqs.court_case_matcher_queue_name}")
    private String queueName;

    @Value("${aws.sqs.process_court_case_matcher_messages:false}")
    private boolean processCourtCaseMatcherMessages;

    @SqsListener(value = "${aws.sqs.crime_portal_gateway_queue_name}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void receiveXml(@NotEmpty String message, @Header("MessageId") String messageId, @Header(value = "operation_Id", required = false) String operationId)
        throws Exception {
        log.info("Received message from SQS queue {} with messageId: {}. Processing flag {}", cpgQueueName, messageId, processCourtCaseMatcherMessages);

        if (!processCourtCaseMatcherMessages) {
            try (final var ignored = telemetryService.withOperation(operationId)) {
                telemetryService.trackSQSMessageEvent(messageId);
                xmlMessageProcessor.process(message, messageId);
            }
        }
    }

    @SqsListener(value = "${aws.sqs.court_case_matcher_queue_name}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void receive(@NotEmpty String message, @Header("MessageId") String messageId, @Header(value = "operation_Id", required = false) String operationId)
        throws Exception {
        log.info("Received JSON message from SQS queue {} with messageId: {}. Processing flag {}", queueName, messageId, processCourtCaseMatcherMessages);
        if (processCourtCaseMatcherMessages) {
            try (final var ignored = telemetryService.withOperation(operationId)) {
                telemetryService.trackSQSMessageEvent(messageId);
                messageProcessor.process(message, messageId);
            }
        }
    }

}
