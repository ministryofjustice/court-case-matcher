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
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchResponse;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
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

        notificationMessagingTemplate.convertAndSend(TOPIC_NAME, hearing, Map.of("messageType", "COMMON_PLATFORM_HEARING","hearingEventType","Resulted"));

        await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> countPutRequestsTo("/hearing/8bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f") == 1);

        MOCK_SERVER.verify(
                putRequestedFor(urlMatching("/hearing/8bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f"))
                        // Values from incoming case
                        .withRequestBody(matchingJsonPath("caseId", equalTo("D517D32D-3C80-41E8-846E-D274DC2B94A5")))
                        .withRequestBody(matchingJsonPath("hearingId", equalTo("8bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f")))
                        .withRequestBody(matchingJsonPath("hearingEventType", equalTo("Resulted")))
                        .withRequestBody(matchingJsonPath("urn", equalTo("25GD34377719")))
                        .withRequestBody(matchingJsonPath("caseNo", equalTo("D517D32D-3C80-41E8-846E-D274DC2B94A5")))
                        .withRequestBody(matchingJsonPath("hearingDays[0].courtCode", equalTo("B10JQ")))
                        .withRequestBody(matchingJsonPath("defendants[0].defendantId", equalTo("0ab7c3e5-eb4c-4e3f-b9e6-b9e78d3ea199")))
                        .withRequestBody(matchingJsonPath("defendants[1].defendantId", equalTo("903c4c54-f667-4770-8fdf-1adbb5957c25")))
                        // Values from court-case-service
                        .withRequestBody(matchingJsonPath("defendants[0].crn", equalTo("X346204")))
                        .withRequestBody(matchingJsonPath("defendants[1].crn", equalTo("X346224")))
                        // Values from probation status update
                        .withRequestBody(matchingJsonPath("defendants[0].phoneNumber.home", equalTo("07000000001")))
                        .withRequestBody(matchingJsonPath("defendants[0].phoneNumber.work", equalTo("07000000002")))
                        .withRequestBody(matchingJsonPath("defendants[0].phoneNumber.mobile", equalTo("07000000003")))
                        .withRequestBody(matchingJsonPath("defendants[0].probationStatus", equalTo("CURRENT")))
                        .withRequestBody(matchingJsonPath("defendants[0].breach", equalTo("false")))
                        .withRequestBody(matchingJsonPath("defendants[0].awaitingPsr", equalTo("true")))
                        .withRequestBody(matchingJsonPath("defendants[1].probationStatus", equalTo("CURRENT")))
                        .withRequestBody(matchingJsonPath("defendants[1].breach", equalTo("true")))
                        .withRequestBody(matchingJsonPath("defendants[1].awaitingPsr", equalTo("false")))
                        .withRequestBody(matchingJsonPath("defendants[0].offences[0].listNo", equalTo("20")))
                        .withRequestBody(matchingJsonPath("defendants[1].offences[1].listNo", equalTo("30")))
                        .withRequestBody(matchingJsonPath("defendants[1].phoneNumber.home", absent()))
                        .withRequestBody(matchingJsonPath("defendants[1].phoneNumber.work", equalTo("07000000005")))
                        .withRequestBody(matchingJsonPath("defendants[1].phoneNumber.mobile", equalTo("07000000006")))
        );

        verify(telemetryService).withOperation(nullable(String.class));
        verify(telemetryService).trackHearingMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackHearingChangedEvent(any(Hearing.class));
        verify(telemetryService, never()).trackOffenderMatchEvent(any(Defendant.class), any(Hearing.class), any(MatchResponse.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @Test
    public void givenExistingCaseWithNoCrn_whenReceivePayload_thenAttemptMatchAndSendExistingCase() throws IOException {
        var hearing = Files.readString(Paths.get(BASE_PATH + "/common-platform/hearing-existing-no-crns.json"));

        notificationMessagingTemplate.convertAndSend(TOPIC_NAME, hearing, Map.of("messageType", "COMMON_PLATFORM_HEARING","hearingEventType","Resulted"));

        await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> countPutRequestsTo("/hearing/f8831ff5-b7d9-455c-b846-444ec8714b8b") == 1);

        MOCK_SERVER.verify(
                putRequestedFor(urlMatching("/hearing/f8831ff5-b7d9-455c-b846-444ec8714b8b"))
                        // Values from incoming case
                        .withRequestBody(matchingJsonPath("caseId", equalTo("8e5cfd34-a8dd-403e-ae96-219704ce7110")))
                        .withRequestBody(matchingJsonPath("hearingId", equalTo("f8831ff5-b7d9-455c-b846-444ec8714b8b")))
                        .withRequestBody(matchingJsonPath("hearingDays[0].courtCode", equalTo("B10JQ")))
                        .withRequestBody(matchingJsonPath("defendants[0].defendantId", equalTo("63259cd9-cb18-4563-b72e-d8baf7c35684")))
                        .withRequestBody(matchingJsonPath("defendants[1].defendantId", equalTo("8e05e32f-8d2c-4782-bcdc-82983099f3fb")))
                        // Values from court-case-service
                        .withRequestBody(matchingJsonPath("defendants[1].crn", equalTo("X198765")))
                        // Values from probation status update
                        .withRequestBody(matchingJsonPath("defendants[1].probationStatus", equalTo("CURRENT")))
                        .withRequestBody(matchingJsonPath("defendants[1].breach", equalTo("true")))
                        .withRequestBody(matchingJsonPath("defendants[1].awaitingPsr", equalTo("false")))
        );

        verify(telemetryService).withOperation(nullable(String.class));
        verify(telemetryService).trackHearingMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackHearingChangedEvent(any(Hearing.class));
        verify(telemetryService, times(2)).trackOffenderMatchEvent(any(Defendant.class), any(Hearing.class), any(MatchResponse.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @Test
    public void givenNewCase_whenReceivePayload_thenSendNewCase() throws IOException {
        var hearing = Files.readString(Paths.get(BASE_PATH + "/common-platform/hearing.json"));

        notificationMessagingTemplate.convertAndSend(TOPIC_NAME, hearing, Map.of("messageType", "COMMON_PLATFORM_HEARING", "hearingEventType","ConfirmedOrUpdated"));

        await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> countPutRequestsTo("/hearing/E10E3EF3-8637-40E3-BDED-8ED104A380AC") == 1);

        MOCK_SERVER.verify(
                putRequestedFor(urlMatching("/hearing/E10E3EF3-8637-40E3-BDED-8ED104A380AC"))
                        // Values from incoming case
                        .withRequestBody(matchingJsonPath("caseId", equalTo("D2B61C8A-0684-4764-B401-F0A788BC7CCF")))
                        .withRequestBody(matchingJsonPath("hearingType", equalTo("sentence")))
                        .withRequestBody(matchingJsonPath("hearingId", equalTo("E10E3EF3-8637-40E3-BDED-8ED104A380AC")))
                        .withRequestBody(matchingJsonPath("hearingEventType", equalTo("ConfirmedOrUpdated")))
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
        verify(telemetryService).trackHearingMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackNewHearingEvent(any(Hearing.class), any(String.class));
        verify(telemetryService, times(2)).trackOffenderMatchEvent(any(Defendant.class), any(Hearing.class), any(MatchResponse.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @Test
    public void givenNewCase_whenReceivePayloadForOrganisation_thenSendNewCase() throws IOException {
        var orgJson = Files.readString(Paths.get(BASE_PATH + "/common-platform/hearing-with-legal-entity-defendant.json"));

        notificationMessagingTemplate.convertAndSend(TOPIC_NAME, orgJson, Map.of("messageType", "COMMON_PLATFORM_HEARING", "hearingEventType","ConfirmedOrUpdated"));

        await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> countPutRequestsTo("/hearing/E10E3EF3-8637-40E3-BDED-8ED104A380AC") == 1);

        MOCK_SERVER.verify(
                putRequestedFor(urlMatching("/hearing/E10E3EF3-8637-40E3-BDED-8ED104A380AC"))
                        .withRequestBody(matchingJsonPath("caseId", equalTo("D2B61C8A-0684-4764-B401-F0A788BC7CCF")))
                        .withRequestBody(matchingJsonPath("hearingId", equalTo("E10E3EF3-8637-40E3-BDED-8ED104A380AC")))
                        .withRequestBody(matchingJsonPath("caseNo", equalTo("D2B61C8A-0684-4764-B401-F0A788BC7CCF")))
                        .withRequestBody(matchingJsonPath("hearingDays[0].courtRoom", equalTo("Crown Court 3-1")))
                        .withRequestBody(matchingJsonPath("defendants[0].type", equalTo("PERSON")))
                        .withRequestBody(matchingJsonPath("defendants[0].defendantId", equalTo("0ab7c3e5-eb4c-4e3f-b9e6-b9e78d3ea199")))
                        .withRequestBody(matchingJsonPath("defendants[0].crn", equalTo("X346204")))
                        .withRequestBody(matchingJsonPath("defendants[1].type", equalTo("ORGANISATION")))
                        .withRequestBody(matchingJsonPath("defendants[1].defendantId", equalTo("903c4c54-f667-4770-8fdf-1adbb5957c25")))
        );

        verify(telemetryService).withOperation(nullable(String.class));
        verify(telemetryService).trackHearingMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackNewHearingEvent(any(Hearing.class), any(String.class));
        verify(telemetryService).trackOffenderMatchEvent(any(Defendant.class), any(Hearing.class), any(MatchResponse.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @Test
    public void givenMatchedExistingCase_whenReceivePayload_thenSendUpdatedCase() throws IOException {
        var hearing = Files.readString(Paths.get(BASE_PATH + "/libra/case.json"));

        notificationMessagingTemplate.convertAndSend(TOPIC_NAME, hearing, Map.of("messageType", "LIBRA_COURT_CASE", "String","Resulted"));

        await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> countPutRequestsTo("/hearing/.*") == 1);

        MOCK_SERVER.verify(
                putRequestedFor(urlMatching("/hearing/.*"))
                        .withRequestBody(matchingJsonPath("defendants[0].pnc", equalTo("2004/0012345U")))
                        .withRequestBody(matchingJsonPath("hearingDays[0].listNo", equalTo("1st")))
                        .withRequestBody(matchingJsonPath("caseNo", equalTo("1600032981")))
                        .withRequestBody(matchingJsonPath("hearingDays[0].courtCode", equalTo("B10JQ")))
        );

        verify(telemetryService).withOperation(nullable(String.class));
        verify(telemetryService).trackHearingMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackNewHearingEvent(any(Hearing.class), any(String.class));
        verify(telemetryService).trackOffenderMatchEvent(any(Defendant.class), any(Hearing.class), any(MatchResponse.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @Test
    public void givenExistingCase_whenReceivePayloadForOrganisation_thenSendUpdatedCase() throws IOException {

        var orgJson = Files.readString(Paths.get(BASE_PATH + "/libra/case-org.json"));

        notificationMessagingTemplate.convertAndSend(TOPIC_NAME, orgJson, Map.of("messageType", "LIBRA_COURT_CASE", "hearingEventType","ConfirmedOrUpdated"));

        final var expectedEndpoint = String.format("/hearing/%s", "A0884637-5A70-4622-88E9-7324949B8E7A");
        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> countPutRequestsTo(expectedEndpoint) == 1);

        MOCK_SERVER.verify(
                putRequestedFor(urlMatching(expectedEndpoint))
                        .withRequestBody(matchingJsonPath("caseId", equalTo("A0884637-5A70-4622-88E9-7324949B8E7A")))
                        .withRequestBody(matchingJsonPath("hearingId", equalTo("A0884637-5A70-4622-88E9-7324949B8E7A")))
                        .withRequestBody(matchingJsonPath("hearingDays[0].courtRoom", equalTo("07")))
                        .withRequestBody(matchingJsonPath("defendants[0].type", equalTo("ORGANISATION")))
                        .withRequestBody(matchingJsonPath("defendants[0].defendantId", equalTo("51EB661C-6CDF-46B2-ACF3-95098CF41154")))
        );

        verify(telemetryService).withOperation(nullable(String.class));
        verify(telemetryService).trackHearingMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackHearingChangedEvent(any(Hearing.class));
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
        @Qualifier("hearingProcessor")
        private HearingProcessor caseMessageProcessor;
        @Autowired
        private HearingExtractor caseExtractor;

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
