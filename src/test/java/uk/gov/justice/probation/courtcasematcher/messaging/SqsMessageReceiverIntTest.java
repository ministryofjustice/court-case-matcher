package uk.gov.justice.probation.courtcasematcher.messaging;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.gateway.Case;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchResponse;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test"})
@Import(TestMessagingConfig.class)
public class SqsMessageReceiverIntTest {

    private static final String BASE_PATH = "src/test/resources/messages";
    private static String singleCase;
    private static String failingCase;

    @Autowired
    private TelemetryService telemetryService;

    @Autowired
    private SqsDlqMessageReceiver dlqMessageReceiver;

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @BeforeAll
    static void beforeAll() throws IOException {
        singleCase = Files.readString(Paths.get(BASE_PATH +"/json/case.json"));
        failingCase = Files.readString(Paths.get(BASE_PATH +"/json/failing-case.json"));
        MOCK_SERVER.start();
    }

    @BeforeEach
    public void beforeEach() {
        dlqMessageReceiver.clearMessages();
    }

    @AfterAll
    static void afterAll() {
        MOCK_SERVER.stop();
    }

    private static final String TOPIC_NAME = "court-case-events-topic";

    @Autowired
    private NotificationMessagingTemplate notificationMessagingTemplate;


    @Test
    public void givenMatchedExistingCase_whenReceivePayload_thenSendUpdatedCase() {

        notificationMessagingTemplate.convertAndSend(TOPIC_NAME, singleCase);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> countPutRequestsTo("/court/B10JQ/case/1600032981") == 1);

        MOCK_SERVER.verify(
            putRequestedFor(urlEqualTo("/court/B10JQ/case/1600032981"))
                .withRequestBody(matchingJsonPath("pnc", equalTo("2004/0012345U")))
                .withRequestBody(matchingJsonPath("listNo", equalTo("1st")))
        );

        verify(telemetryService).withOperation(nullable(String.class));
        verify(telemetryService).trackCaseMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackCourtCaseEvent(any(Case.class), any(String.class));
        verify(telemetryService).trackOffenderMatchEvent(any(CourtCase.class), any(MatchResponse.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @Test
    public void givenExistingCase_whenReceivePayloadForOrganisation_thenSendUpdatedCase() throws IOException {

        var orgJson = Files.readString(Paths.get(BASE_PATH +"/json/case-org.json"));

        notificationMessagingTemplate.convertAndSend(TOPIC_NAME, orgJson);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> countPutRequestsTo("/court/B10JQ/case/2100049401") == 1);

        MOCK_SERVER.verify(
            putRequestedFor(urlEqualTo("/court/B10JQ/case/2100049401"))
                .withRequestBody(matchingJsonPath("courtRoom", equalTo("07")))
                .withRequestBody(matchingJsonPath("defendantType", equalTo("ORGANISATION")))
        );

        verify(telemetryService).withOperation(nullable(String.class));
        verify(telemetryService).trackCaseMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackCourtCaseEvent(any(Case.class), any(String.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @Test
    public void givenCaseFailsAtCCS_whenReceivePayload_thenPlaceOnDLQ() {

        notificationMessagingTemplate.convertAndSend(TOPIC_NAME, failingCase);


        await()
                .atMost(60, TimeUnit.SECONDS)
                .until(() -> {
                    System.out.println(dlqMessageReceiver.getMessages().size());
                    return dlqMessageReceiver.getMessages().size() == 1;
                });

        MOCK_SERVER.verify(
            putRequestedFor(urlEqualTo("/court/B10JQ/case/666666"))
                .withRequestBody(matchingJsonPath("caseNo", equalTo("666666")))
                .withRequestBody(matchingJsonPath("crn", equalTo("X500")))
        );
        assertThat(dlqMessageReceiver.getMessages().size()).isEqualTo(1);
        assertThat(dlqMessageReceiver.getMessages().get(0)).contains("666666");
    }

    @TestConfiguration
    public static class AwsTestConfig {
        @Value("${aws.sqs.court_case_matcher_endpoint_url}")
        private String sqsEndpointUrl;
        @Value("${aws.access_key_id}")
        private String accessKeyId;
        @Value("${aws.secret_access_key}")
        private String secretAccessKey;
        @Value("${aws.region_name}")
        private String regionName;
        @Value("${aws.sqs.court_case_matcher_queue_name}")
        private String queueName;
//        @Value("${aws.sqs.court_case_matcher_dlq_name}")
//        private String dlqName;
        @MockBean
        private TelemetryService telemetryService;
        @Autowired
        @Qualifier("caseMessageProcessor")
        private MessageProcessor caseMessageProcessor;

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
        public AmazonSNS amazonSNSClient() {
            return AmazonSNSClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretAccessKey)))
                .withEndpointConfiguration(new EndpointConfiguration(sqsEndpointUrl, regionName))
                .build();
        }

        @Bean
        public NotificationMessagingTemplate notificationMessagingTemplate(AmazonSNS amazonSNS) {
            return new NotificationMessagingTemplate(amazonSNS);
        }

        @Bean
        public SqsMessageReceiver sqsMessageReceiver() {
            return new SqsMessageReceiver(caseMessageProcessor, telemetryService, queueName);
        }
    }

    public int countPutRequestsTo(final String url) {
        return MOCK_SERVER.findAll(putRequestedFor(urlEqualTo(url))).size();
    }

}
