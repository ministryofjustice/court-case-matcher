package uk.gov.justice.probation.courtcasematcher.controller;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"local", "sqsadmin"})
@Import(TestMessagingConfig.class)
@DirtiesContext
public class SqsAdminControllerIntTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @LocalServerPort
    protected int port;

    @Autowired
    @Qualifier("courtCaseMatcherSqsQueue")
    private AmazonSQSAsync courtCaseMatcherSqsQueue;

    @Autowired
    @Qualifier("courtCaseMatcherSqsDlq")
    private AmazonSQSAsync courtCaseMatcherSqsDlq;

    @Value("${aws.sqs.court_case_matcher_endpoint_url}")
    private String courtCaseMatcherSqsEndpointUrl;

    @Value("${aws.sqs.court_case_matcher_dlq_endpoint_url}")
    private String courtCaseMatcherSqsDlqEndpointUrl;

    @Test
    void givenThereAreMessagesOnDlq_whenRetryAllDlqInvoked_shouldReplayMessages() {

        purgeQueues();

        sendMessageToDlq("message body 1");
        sendMessageToDlq("message body 2");

        String sqsAdminUrl = "http://localhost:" + port + "/queue-admin/retry-all-dlqs";
        var response = testRestTemplate.exchange(sqsAdminUrl, HttpMethod.PUT, null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var messageResult = courtCaseMatcherSqsQueue.receiveMessage(new ReceiveMessageRequest(courtCaseMatcherSqsEndpointUrl).withMaxNumberOfMessages(2));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages.size()).isEqualTo(2);
        assertThat( messages).extracting(Message::getBody).containsExactlyInAnyOrder("message body 1", "message body 2");

        var dlqMessageResult = courtCaseMatcherSqsDlq.receiveMessage(new ReceiveMessageRequest(courtCaseMatcherSqsDlqEndpointUrl).withMaxNumberOfMessages(2));
        assertThat(dlqMessageResult.getMessages().size()).isEqualTo(0);

        purgeQueues();
    }

    private void purgeQueues() {
        courtCaseMatcherSqsQueue.purgeQueue(new PurgeQueueRequest(courtCaseMatcherSqsEndpointUrl));
        courtCaseMatcherSqsDlq.purgeQueue(new PurgeQueueRequest(courtCaseMatcherSqsDlqEndpointUrl));
    }

    private SendMessageResult sendMessageToDlq(String messageBody) {
        return courtCaseMatcherSqsDlq.sendMessage(new SendMessageRequest().withQueueUrl(courtCaseMatcherSqsDlqEndpointUrl).withMessageBody(messageBody));
    }
}


