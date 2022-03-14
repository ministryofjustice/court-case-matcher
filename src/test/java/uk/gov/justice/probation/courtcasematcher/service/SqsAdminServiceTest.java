package uk.gov.justice.probation.courtcasematcher.service;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.List.of;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsAdminServiceTest {

    private static String COURTCASE_MATCHER_SQS_DLQ_URL = "http://dlq";
    private static String COURTCASE_MATCHER_SQS_URL = "http://queue";

    @Mock
    private AmazonSQSAsync courtCaseMatcherSqsDlq;

    @Mock
    private AmazonSQSAsync courtCaseMatcherSqsQueue;

    private SqsAdminService sqsAdminService;

    @BeforeEach
    void prepare() {
        sqsAdminService = new SqsAdminService(courtCaseMatcherSqsDlq, courtCaseMatcherSqsQueue,
                COURTCASE_MATCHER_SQS_DLQ_URL, COURTCASE_MATCHER_SQS_URL);
    }

    @Test
    void shouldReplayMessagesFromDlq() {
        when(courtCaseMatcherSqsDlq.getQueueAttributes(COURTCASE_MATCHER_SQS_DLQ_URL, of("ApproximateNumberOfMessages")))
                .thenReturn(new GetQueueAttributesResult().addAttributesEntry("ApproximateNumberOfMessages", "2"));
        String messageBody = "body one";
        when(courtCaseMatcherSqsDlq.receiveMessage(new ReceiveMessageRequest(COURTCASE_MATCHER_SQS_DLQ_URL).withMaxNumberOfMessages(1)))
                .thenReturn(new ReceiveMessageResult().withMessages(new Message().withBody(messageBody).withReceiptHandle("receipt-handle")));
        sqsAdminService.replayDlqMessages();
        verify(courtCaseMatcherSqsDlq, times(2)).receiveMessage(
                new ReceiveMessageRequest(COURTCASE_MATCHER_SQS_DLQ_URL).withMaxNumberOfMessages(1));
        verify(courtCaseMatcherSqsQueue, times(2)).sendMessage(COURTCASE_MATCHER_SQS_URL, messageBody);
        verify(courtCaseMatcherSqsDlq, times(2)).deleteMessage(
                new DeleteMessageRequest(COURTCASE_MATCHER_SQS_DLQ_URL, "receipt-handle"));
    }

    @Test
    void shouldSkipReplayMessagesFromDlqWhenNoMessages() {
        when(courtCaseMatcherSqsDlq.getQueueAttributes(COURTCASE_MATCHER_SQS_DLQ_URL, of("ApproximateNumberOfMessages")))
                .thenReturn(new GetQueueAttributesResult().addAttributesEntry("ApproximateNumberOfMessages", "0"));
        sqsAdminService.replayDlqMessages();
        verify(courtCaseMatcherSqsDlq, times(0)).receiveMessage(
                new ReceiveMessageRequest(COURTCASE_MATCHER_SQS_DLQ_URL).withMaxNumberOfMessages(1));
        verifyNoInteractions(courtCaseMatcherSqsQueue);
    }
}