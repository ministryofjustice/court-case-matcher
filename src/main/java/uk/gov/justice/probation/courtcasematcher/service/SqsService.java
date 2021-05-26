package uk.gov.justice.probation.courtcasematcher.service;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class SqsService {

    private final String queueName;

    private final AmazonSQSAsync amazonSQSAsync;

    public boolean isQueueAvailable() {
        try {
            return checkQueue(queueName, amazonSQSAsync);// && checkQueue(cpqQueueName, cpgAmazonSQSAsync);
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
