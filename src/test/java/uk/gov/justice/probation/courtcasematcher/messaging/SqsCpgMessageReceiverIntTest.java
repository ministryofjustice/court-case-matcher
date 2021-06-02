package uk.gov.justice.probation.courtcasematcher.messaging;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchResponse;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test"})
@Import(TestMessagingConfig.class)
public class SqsCpgMessageReceiverIntTest {

    private static final String BASE_PATH = "src/test/resources/messages";
    private static String singleCaseXml;

    @Autowired
    private TelemetryService telemetryService;

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @BeforeAll
    static void beforeAll() throws IOException {
        singleCaseXml = Files.readString(Paths.get(BASE_PATH +"/xml/external-document-request-single-case.xml"));
        MOCK_SERVER.start();
    }

    @AfterAll
    static void afterAll() {
        MOCK_SERVER.stop();
    }

    private static final String CPQ_QUEUE_NAME = "crime-portal-gateway-queue";

    @Autowired
    private QueueMessagingTemplate queueMessagingTemplate;

    @Test
    public void givenMatchedExistingCase_whenReceivePayload_thenSendUpdatedCase() {

        queueMessagingTemplate.convertAndSend(CPQ_QUEUE_NAME, singleCaseXml, Map.of("operation_Id", "operationId"));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> countPutRequestsTo("/court/B10JQ/case/1600032981") == 1);

        MOCK_SERVER.verify(
            putRequestedFor(urlEqualTo("/court/B10JQ/case/1600032981"))
                .withRequestBody(matchingJsonPath("pnc", equalTo("2004/0012345U")))
                .withRequestBody(matchingJsonPath("probationStatus", equalTo("CURRENT")))
                .withRequestBody(matchingJsonPath("listNo", equalTo("2nd")))
        );

        verify(telemetryService).withOperation("operationId");
        verify(telemetryService).trackCourtListMessageEvent(any(String.class));
        verify(telemetryService).trackCourtCaseEvent(any(Case.class), any(String.class));
        verify(telemetryService).trackOffenderMatchEvent(any(CourtCase.class), any(MatchResponse.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @Test
    public void givenExistingCase_whenReceivePayloadForOrganisation_thenSendUpdatedCase() throws IOException {

        var orgXml = Files.readString(Paths.get(BASE_PATH +"/xml/external-document-request-single-case-org.xml"));

        queueMessagingTemplate.convertAndSend(CPQ_QUEUE_NAME, orgXml, Map.of("operation_Id", "operationId"));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> countPutRequestsTo("/court/B10JQ/case/2100049401") == 1);

        MOCK_SERVER.verify(
            putRequestedFor(urlEqualTo("/court/B10JQ/case/2100049401"))
                .withRequestBody(matchingJsonPath("courtRoom", equalTo("7")))
                .withRequestBody(matchingJsonPath("defendantType", equalTo("ORGANISATION")))
        );

        verify(telemetryService).withOperation("operationId");
        verify(telemetryService).trackCourtListMessageEvent(any(String.class));
        verify(telemetryService).trackCourtCaseEvent(any(Case.class), any(String.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @TestConfiguration
    public static class AwsTestConfig {
        @Value("${aws.sqs.crime_portal_gateway_endpoint_url}")
        private String sqsEndpointUrl;
        @Value("${aws.sqs.crime_portal_gateway_access_key_id}")
        private String accessKeyId;
        @Value("${aws.sqs.crime_portal_gateway_secret_access_key}")
        private String secretAccessKey;
        @Value("${aws.region_name}")
        private String regionName;
        @Value("${aws.sqs.crime_portal_gateway_queue_name}")
        private String cpgQueueName;
        @MockBean
        private TelemetryService telemetryService;
        @Autowired
        @Qualifier("externalDocumentMessageProcessor")
        private MessageProcessor externalDocumentMessageProcessor;

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
        public SqsCpgMessageReceiver sqsMessageReceiver() {
            return new SqsCpgMessageReceiver(externalDocumentMessageProcessor, telemetryService, cpgQueueName);
        }

        @Bean
        public QueueMessagingTemplate queueMessagingTemplate(@Autowired AmazonSQSAsync amazonSQSAsync) {
            return new QueueMessagingTemplate(amazonSQSAsync);
        }
    }

    public int countPutRequestsTo(final String url) {
        return MOCK_SERVER.findAll(putRequestedFor(urlEqualTo(url))).size();
    }

}
