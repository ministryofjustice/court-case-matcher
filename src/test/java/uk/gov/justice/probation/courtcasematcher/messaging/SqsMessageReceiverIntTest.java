package uk.gov.justice.probation.courtcasematcher.messaging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "test-msg"})
public class SqsMessageReceiverIntTest {

    private static String singleCaseXml;

    @Autowired
    private MessageProcessor messageProcessor;

    @Autowired
    private TelemetryService telemetryService;

    @BeforeAll
    static void beforeAll() throws IOException {
        final String basePath = "src/test/resources/messages";
        singleCaseXml = Files.readString(Paths.get(basePath +"/external-document-request-single-case.xml"));
    }

    private static final String QUEUE_NAME = "crime-portal-gateway-queue";

    @Autowired
    private QueueMessagingTemplate queueMessagingTemplate;

    @Test
    public void whenReceivePayload_thenSendCase() {

        queueMessagingTemplate.convertAndSend(QUEUE_NAME, singleCaseXml);

        Mockito.verify(telemetryService, Mockito.timeout(120000)).trackSQSMessageEvent(any(String.class));
        Mockito.verify(messageProcessor, Mockito.timeout(120000)).process(any(ExternalDocumentRequest.class), any(String.class));
        Mockito.verifyNoMoreInteractions(telemetryService, messageProcessor);
    }

    @TestConfiguration
    public static class AwsTestConfig {
        @Value("${aws.sqs-endpoint-url}")
        private String sqsEndpointUrl;
        @Value("${aws.access_key_id}")
        private String accessKeyId;
        @Value("${aws.secret_access_key}")
        private String secretAccessKey;
        @Value("${aws.region_name}")
        private String regionName;
        @Value("${messaging.sqs.queue_name}")
        private String queueName;

        @MockBean
        private EventBus eventBus;
        @MockBean
        private TelemetryService telemetryService;
        @Autowired
        private MessageParser messageParser;
        @MockBean
        private MessageProcessor messageProcessor;

        @Primary
        @Bean
        public AmazonSQSAsync amazonSQSAsync() {
            return AmazonSQSAsyncClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretAccessKey)))
                .withEndpointConfiguration(new EndpointConfiguration(sqsEndpointUrl, regionName))
                .build();
        }

        @Bean
        public SqsMessageReceiver sqsMessageReceiver() {
            return new SqsMessageReceiver(messageProcessor, telemetryService, eventBus, messageParser, queueName);
        }

        @Bean
        public QueueMessagingTemplate queueMessagingTemplate(@Autowired AmazonSQSAsync amazonSQSAsync) {
            return new QueueMessagingTemplate(amazonSQSAsync);
        }
    }
}
