package uk.gov.justice.probation.courtcasematcher.messaging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-msg")
//@Disabled
public class SqsMessageReceiverIntTest {

    private static String singleCaseXml;

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @MockBean
    private TelemetryService telemetryService;

    @Autowired
    private AmazonSQSAsync amazonSQSAsync;

    @Container
    static final LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack"))
        .withServices(SQS)
        .withEnv("DEFAULT_REGION", "eu-west-2")
        .withEnv("HOSTNAME_EXTERNAL", "localhost");

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", QUEUE_NAME);
        final String basePath = "src/test/resources/messages";
        singleCaseXml = Files.readString(Paths.get(basePath +"/external-document-request-single-case.xml"));
    }

    @BeforeEach
    void beforeEach()  {
        this.queueMessagingTemplate = new QueueMessagingTemplate(amazonSQSAsync);
    }

    private static final String QUEUE_NAME = "crime-portal-gateway-queue";

    private QueueMessagingTemplate queueMessagingTemplate;

    @Test
    public void whenReceivePayload_thenSendCase() {

        queueMessagingTemplate.convertAndSend(QUEUE_NAME, singleCaseXml);

        Awaitility.given()
            .await()
            .atMost(60, SECONDS)
            .untilAsserted(() -> telemetryService.trackSQSMessageEvent("sds"));
    }

    @ActiveProfiles("test-msg")
    @TestConfiguration
    public static class AwsTestConfig {

        @Primary
        @Bean
        public AmazonSQSAsync amazonSQSAsync() {
            return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(localStack.getDefaultCredentialsProvider())
                .withEndpointConfiguration(localStack.getEndpointConfiguration(SQS))
                .build();
        }

        @Bean
        public QueueMessagingTemplate queueMessagingTemplate(@Autowired AmazonSQSAsync amazonSQSAsync) {
            return new QueueMessagingTemplate(amazonSQSAsync);
        }
    }
}
