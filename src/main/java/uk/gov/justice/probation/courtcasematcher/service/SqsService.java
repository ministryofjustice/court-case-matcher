package uk.gov.justice.probation.courtcasematcher.service;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Profile("sqs-messaging")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Component
public class SqsService {

    @Value("${aws_sqs_court_case_matcher_queue_name:court-case-matcher-queue}")
    private String queueName;

    @Autowired
    @Qualifier("amazonSQSAsync")
    private AmazonSQSAsync amazonSQSAsync;

    @Value("${aws_sqs_crime_portal_gateway_queue_name:crime-portal-gateway-queue}")
    private String cpqQueueName;

    @Autowired
    @Qualifier("cpgAmazonSQSAsync")
    private AmazonSQSAsync cpgAmazonSQSAsync;

    public boolean isQueueAvailable() {
        try {
            return checkQueue(queueName, amazonSQSAsync) && checkQueue(cpqQueueName, cpgAmazonSQSAsync);
        }
        catch (QueueDoesNotExistException existException) {
            return false;
        }
    }

    private boolean checkQueue(String queueName, AmazonSQSAsync amazonSQS) {
        final var queueUrlResult = amazonSQS.getQueueUrl(queueName);
        return queueUrlResult != null && !ObjectUtils.isEmpty(queueUrlResult.getQueueUrl());
    }

}
