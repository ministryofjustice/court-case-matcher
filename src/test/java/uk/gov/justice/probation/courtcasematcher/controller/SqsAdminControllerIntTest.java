package uk.gov.justice.probation.courtcasematcher.controller;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"local", "sqsadmin", "unsecured"})
@DirtiesContext
public class SqsAdminControllerIntTest {

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


    @Disabled("until reason for failure is established")
    void givenThereAreMessagesOnDlq_whenRetryAllDlqInvoked_shouldReplayMessages() {

        purgeQueues();

        sendMessageToDlq("message body 1");
        sendMessageToDlq("message body 2");

        String sqsAdminUrl = String.format("http://localhost:%d/queue-admin/retry-all-dlqs", port);
        final var response = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .put(sqsAdminUrl)
                .then()
                .statusCode(200);

        var messageResult = courtCaseMatcherSqsQueue.receiveMessage(new ReceiveMessageRequest(courtCaseMatcherSqsEndpointUrl).withMaxNumberOfMessages(2));
        List<Message> messages = messageResult.getMessages();
        assertThat(messages.size()).isEqualTo(2);
        assertThat(messages).extracting(Message::getBody).containsExactlyInAnyOrder("message body 1", "message body 2");

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


