package uk.gov.justice.probation.courtcasematcher.messaging;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Profile("test")
public class SqsDlqMessageReceiver {

    @Getter
    private final List<String> messages = new ArrayList<>(1);

    @Value("${aws.sqs.court_case_matcher_queue_name}")
    private String queueName;

    @SqsListener(value = "${aws.sqs.court_case_matcher_dlq_name}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void receive(@NotEmpty String message, @Header("MessageId") String messageId) {
        log.info("Received JSON message from SQS DLQ {} with messageId: {}. ", queueName, messageId);
        messages.add(message);
    }

    public void clearMessages() {
        messages.clear();
    }
}
