package uk.gov.justice.probation.courtcasematcher.service;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static java.util.List.of;
import static java.util.Optional.ofNullable;

@Service
@Slf4j
public class SqsAdminService {
    private static final String APPROXIMATE_NUMBER_OF_MESSAGES_ATTR_NAME = "ApproximateNumberOfMessages";

    private final AmazonSQSAsync courtCaseMatcherSqsDlq;

    private final AmazonSQSAsync courtCaseMatcherSqsQueue;

    private final String courtCaseMatcherSqsDlqUrl;

    private final String courtCaseMatcherSqsUrl;

    public SqsAdminService(@Qualifier("courtCaseMatcherSqsDlq") AmazonSQSAsync courtCaseMatcherSqsDlq,
                           @Qualifier("courtCaseMatcherSqsQueue") AmazonSQSAsync courtCaseMatcherSqsQueue,
                           @Value("${aws.sqs.court_case_matcher_dlq_endpoint_url}") String courtCaseMatcherSqsDlqUrl,
                           @Value("${aws.sqs.court_case_matcher_endpoint_url}") String courtCaseMatcherSqsUrl) {
        this.courtCaseMatcherSqsDlq = courtCaseMatcherSqsDlq;
        this.courtCaseMatcherSqsQueue = courtCaseMatcherSqsQueue;
        this.courtCaseMatcherSqsDlqUrl = courtCaseMatcherSqsDlqUrl;
        this.courtCaseMatcherSqsUrl = courtCaseMatcherSqsUrl;
    }

    public void replayDlqMessages() {

        log.info("Replaying court case matcher dlq messages.");

        var dlqAttributes = courtCaseMatcherSqsDlq.getQueueAttributes(courtCaseMatcherSqsDlqUrl,
                of(APPROXIMATE_NUMBER_OF_MESSAGES_ATTR_NAME));
        var messageCount = ofNullable(dlqAttributes.getAttributes())
                .map(attrMap -> attrMap.get(APPROXIMATE_NUMBER_OF_MESSAGES_ATTR_NAME))
                .map(Integer::parseInt)
                .orElse(0);

        log.info("Found {} messages on the dlq", messageCount);

        var replayedMessages = 0;

        while (replayedMessages < messageCount) {
            var dlqMessages = courtCaseMatcherSqsDlq.receiveMessage(
                    new ReceiveMessageRequest(courtCaseMatcherSqsDlqUrl).withMaxNumberOfMessages(1))
                    .getMessages();
            dlqMessages.stream().forEach(message -> {
                courtCaseMatcherSqsQueue.sendMessage(courtCaseMatcherSqsUrl, message.getBody());
                courtCaseMatcherSqsDlq.deleteMessage(new DeleteMessageRequest(courtCaseMatcherSqsDlqUrl, message.getReceiptHandle()));
            });
            replayedMessages++;
        }

        log.info("Replayed {} messages.", replayedMessages);
    }
}
