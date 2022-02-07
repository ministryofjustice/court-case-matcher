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
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchResponse;
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
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test"})
@Import(TestMessagingConfig.class)
public class SqsMessageReceiverIntTest {

    private static final String BASE_PATH = "src/test/resources/messages";

    @Autowired
    private TelemetryService telemetryService;

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @BeforeAll
    static void beforeAll() {
        MOCK_SERVER.start();
    }

    @AfterAll
    static void afterAll() {
        MOCK_SERVER.stop();
    }

    private static final String TOPIC_NAME = "court-case-events-topic";

    @Autowired
    private NotificationMessagingTemplate notificationMessagingTemplate;

    @Test
    public void givenExistingCase_whenReceivePayload_thenSendExistingCase() throws IOException {
        var hearing = Files.readString(Paths.get(BASE_PATH + "/common-platform/hearing-existing.json"));

        notificationMessagingTemplate.convertAndSend(TOPIC_NAME, hearing, Map.of("messageType", "COMMON_PLATFORM_HEARING"));

        await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> countPutRequestsTo("/case/D517D32D-3C80-41E8-846E-D274DC2B94A5/extended") == 1);

        MOCK_SERVER.verify(
                putRequestedFor(urlMatching("/case/D517D32D-3C80-41E8-846E-D274DC2B94A5/extended"))
                        // Values from incoming case
                        .withRequestBody(matchingJsonPath("caseId", equalTo("D517D32D-3C80-41E8-846E-D274DC2B94A5")))
                        .withRequestBody(matchingJsonPath("caseNo", equalTo("D517D32D-3C80-41E8-846E-D274DC2B94A5")))
                        .withRequestBody(matchingJsonPath("hearingDays[0].courtCode", equalTo("B10JQ")))
                        .withRequestBody(matchingJsonPath("defendants[0].defendantId", equalTo("0ab7c3e5-eb4c-4e3f-b9e6-b9e78d3ea199")))
                        .withRequestBody(matchingJsonPath("defendants[1].defendantId", equalTo("903c4c54-f667-4770-8fdf-1adbb5957c25")))
                        // Values from court-case-service
                        .withRequestBody(matchingJsonPath("defendants[0].crn", equalTo("X346204")))
                        .withRequestBody(matchingJsonPath("defendants[1].crn", equalTo("X346224")))
                        // Values from probation status update
                        .withRequestBody(matchingJsonPath("defendants[0].probationStatus", equalTo("CURRENT")))
                        .withRequestBody(matchingJsonPath("defendants[0].breach", equalTo("false")))
                        .withRequestBody(matchingJsonPath("defendants[0].awaitingPsr", equalTo("true")))
                        .withRequestBody(matchingJsonPath("defendants[1].probationStatus", equalTo("CURRENT")))
                        .withRequestBody(matchingJsonPath("defendants[1].breach", equalTo("true")))
                        .withRequestBody(matchingJsonPath("defendants[1].awaitingPsr", equalTo("false")))
                        .withRequestBody(matchingJsonPath("defendants[0].offences[0].listNo", equalTo("20")))
                        .withRequestBody(matchingJsonPath("defendants[1].offences[1].listNo", equalTo("30")))
        );

        verify(telemetryService).withOperation(nullable(String.class));
        verify(telemetryService).trackCaseMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackCourtCaseEvent(any(CourtCase.class), any(String.class));
        verify(telemetryService, never()).trackOffenderMatchEvent(any(Defendant.class), any(CourtCase.class), any(MatchResponse.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @Test
    public void givenNewCase_whenReceivePayload_thenSendNewCase() throws IOException {
        var hearing = Files.readString(Paths.get(BASE_PATH + "/common-platform/hearing.json"));

        notificationMessagingTemplate.convertAndSend(TOPIC_NAME, hearing, Map.of("messageType", "COMMON_PLATFORM_HEARING"));

        await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> countPutRequestsTo("/case/D2B61C8A-0684-4764-B401-F0A788BC7CCF/extended") == 1);

        MOCK_SERVER.verify(
                putRequestedFor(urlMatching("/case/D2B61C8A-0684-4764-B401-F0A788BC7CCF/extended"))
                        // Values from incoming case
                        .withRequestBody(matchingJsonPath("caseId", equalTo("D2B61C8A-0684-4764-B401-F0A788BC7CCF")))
                        .withRequestBody(matchingJsonPath("caseNo", equalTo("D2B61C8A-0684-4764-B401-F0A788BC7CCF")))
                        .withRequestBody(matchingJsonPath("hearingDays[0].courtCode", equalTo("B10JQ")))
                        .withRequestBody(matchingJsonPath("defendants[0].defendantId", equalTo("0ab7c3e5-eb4c-4e3f-b9e6-b9e78d3ea199")))
                        .withRequestBody(matchingJsonPath("defendants[0].pnc", equalTo("2004/0012345U")))
                        .withRequestBody(matchingJsonPath("defendants[1].defendantId", equalTo("903c4c54-f667-4770-8fdf-1adbb5957c25")))
                        // Values from offender search
                        .withRequestBody(matchingJsonPath("defendants[0].crn", equalTo("X346204")))
                        .withRequestBody(matchingJsonPath("defendants[1].crn", equalTo("X346205")))
                        .withRequestBody(matchingJsonPath("defendants[0].offences[0].listNo", equalTo("30")))
        );

        verify(telemetryService).withOperation(nullable(String.class));
        verify(telemetryService).trackCaseMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackCourtCaseEvent(any(CourtCase.class), any(String.class));
        verify(telemetryService, times(2)).trackOffenderMatchEvent(any(Defendant.class), any(CourtCase.class), any(MatchResponse.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @Test
    public void givenNewCase_whenReceivePayloadForOrganisation_thenSendNewCase() throws IOException {
        var orgJson = Files.readString(Paths.get(BASE_PATH + "/common-platform/hearing-with-legal-entity-defendant.json"));

        notificationMessagingTemplate.convertAndSend(TOPIC_NAME, orgJson, Map.of("messageType", "COMMON_PLATFORM_HEARING"));

        await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> countPutRequestsTo("/case/D2B61C8A-0684-4764-B401-F0A788BC7CCF/extended") == 1);

        MOCK_SERVER.verify(
                putRequestedFor(urlMatching("/case/D2B61C8A-0684-4764-B401-F0A788BC7CCF/extended"))
                        .withRequestBody(matchingJsonPath("caseId", equalTo("D2B61C8A-0684-4764-B401-F0A788BC7CCF")))
                        .withRequestBody(matchingJsonPath("caseNo", equalTo("D2B61C8A-0684-4764-B401-F0A788BC7CCF")))
                        .withRequestBody(matchingJsonPath("hearingDays[0].courtRoom", equalTo("Crown Court 3-1")))
                        .withRequestBody(matchingJsonPath("defendants[0].type", equalTo("PERSON")))
                        .withRequestBody(matchingJsonPath("defendants[0].defendantId", equalTo("0ab7c3e5-eb4c-4e3f-b9e6-b9e78d3ea199")))
                        .withRequestBody(matchingJsonPath("defendants[0].crn", equalTo("X346204")))
                        .withRequestBody(matchingJsonPath("defendants[1].type", equalTo("ORGANISATION")))
                        .withRequestBody(matchingJsonPath("defendants[1].defendantId", equalTo("903c4c54-f667-4770-8fdf-1adbb5957c25")))
        );

        verify(telemetryService).withOperation(nullable(String.class));
        verify(telemetryService).trackCaseMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackCourtCaseEvent(any(CourtCase.class), any(String.class));
        verify(telemetryService).trackOffenderMatchEvent(any(Defendant.class), any(CourtCase.class), any(MatchResponse.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @Test
    public void givenMatchedExistingCase_whenReceivePayload_thenSendUpdatedCase() throws IOException {
        var hearing = Files.readString(Paths.get(BASE_PATH + "/libra/case.json"));

        notificationMessagingTemplate.convertAndSend(TOPIC_NAME, hearing, Map.of("messageType", "LIBRA_COURT_CASE"));

        await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> countPutRequestsTo("/case/.*/extended") == 1);

        MOCK_SERVER.verify(
                putRequestedFor(urlMatching("/case/.*/extended"))
                        .withRequestBody(matchingJsonPath("defendants[0].pnc", equalTo("2004/0012345U")))
                        .withRequestBody(matchingJsonPath("hearingDays[0].listNo", equalTo("1st")))
                        .withRequestBody(matchingJsonPath("caseNo", equalTo("1600032981")))
                        .withRequestBody(matchingJsonPath("hearingDays[0].courtCode", equalTo("B10JQ")))
        );

        verify(telemetryService).withOperation(nullable(String.class));
        verify(telemetryService).trackCaseMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackCourtCaseEvent(any(CourtCase.class), any(String.class));
        verify(telemetryService).trackOffenderMatchEvent(any(Defendant.class), any(CourtCase.class), any(MatchResponse.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @Test
    public void givenExistingCase_whenReceivePayloadForOrganisation_thenSendUpdatedCase() throws IOException {

        var orgJson = Files.readString(Paths.get(BASE_PATH + "/libra/case-org.json"));

        notificationMessagingTemplate.convertAndSend(TOPIC_NAME, orgJson, Map.of("messageType", "LIBRA_COURT_CASE"));

        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> countPutRequestsTo("/case/.*/extended") == 1);

        MOCK_SERVER.verify(
                putRequestedFor(urlMatching("/case/.*/extended"))
                        .withRequestBody(matchingJsonPath("caseId", equalTo("A0884637-5A70-4622-88E9-7324949B8E7A")))
                        .withRequestBody(matchingJsonPath("hearingDays[0].courtRoom", equalTo("07")))
                        .withRequestBody(matchingJsonPath("defendants[0].type", equalTo("ORGANISATION")))
                        .withRequestBody(matchingJsonPath("defendants[0].defendantId", equalTo("51EB661C-6CDF-46B2-ACF3-95098CF41154")))
        );

        verify(telemetryService).withOperation(nullable(String.class));
        verify(telemetryService).trackCaseMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackCourtCaseEvent(any(CourtCase.class), any(String.class));
        verifyNoMoreInteractions(telemetryService);
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
        @MockBean
        private TelemetryService telemetryService;
        @Autowired
        @Qualifier("courtCaseProcessor")
        private CourtCaseProcessor caseMessageProcessor;
        @Autowired
        private CourtCaseExtractor caseExtractor;

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
            return new SqsMessageReceiver(caseMessageProcessor, telemetryService, queueName, caseExtractor);
        }
    }

    public int countPutRequestsTo(final String url) {
        return MOCK_SERVER.findAll(putRequestedFor(urlMatching(url))).size();
    }

}
