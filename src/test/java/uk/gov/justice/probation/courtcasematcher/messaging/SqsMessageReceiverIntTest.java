package uk.gov.justice.probation.courtcasematcher.messaging;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import uk.gov.justice.hmpps.sqs.HmppsQueue;
import uk.gov.justice.hmpps.sqs.HmppsQueueService;
import uk.gov.justice.hmpps.sqs.HmppsTopic;
import uk.gov.justice.hmpps.sqs.HmppsTopicKt;
import uk.gov.justice.probation.courtcasematcher.application.FeatureFlags;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test"})
@Import(TestMessagingConfig.class)
public class SqsMessageReceiverIntTest {
    RetryPolicy DEFAULT_RETRY_POLICY = new SimpleRetryPolicy();
    BackOffPolicy DEFAULT_BACKOFF_POLICY = new ExponentialBackOffPolicy();
    private static final String BASE_PATH = "src/test/resources/messages";

    @Autowired
    private TelemetryService telemetryService;

    @Autowired
    private FeatureFlags featureFlags;

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @Autowired
    private HmppsQueueService hmppsQueueService;

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.large-hearings.bucket-name}")
    private String s3LargeHearingBucket;

    @Value("${commonplatform.event.type.large}")
    private String largeEventType;
    
    @Value("${commonplatform.event.type.default}")
    private String eventType;

    @BeforeAll
    static void beforeAll() {
        MOCK_SERVER.start();
    }

    @AfterAll
    static void afterAll() {
        MOCK_SERVER.stop();
    }
    private static final String TOPIC_NAME = "courtcasestopic";
    HmppsTopic topic;
    @BeforeEach
    void setUp(){
        topic = hmppsQueueService.findByTopicId(TOPIC_NAME);
        HmppsQueue queue = hmppsQueueService.findByQueueId("courtcasesqueue");

        queue.getSqsClient().purgeQueue(PurgeQueueRequest.builder().queueUrl(queue.getQueueUrl()).build());
        queue.getSqsDlqClient().purgeQueue(PurgeQueueRequest.builder().queueUrl(queue.getDlqUrl()).build());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void givenExistingCase_whenReceivePayload_thenSendUpdatedExistingCase(boolean matchOnEveryRecordUpdate) throws IOException {
        featureFlags.setFlagValue("match-on-every-no-record-update", matchOnEveryRecordUpdate);
        var hearing = Files.readString(Paths.get(BASE_PATH + "/common-platform/hearing-existing.json"));
        publishMessage(hearing, Map.of("eventType",
            MessageAttributeValue.builder().dataType("String").stringValue(eventType).build(),
            "messageType", MessageAttributeValue.builder().dataType("String").stringValue("COMMON_PLATFORM_HEARING").build(), "hearingEventType", MessageAttributeValue.builder().dataType("String").stringValue("Resulted").build()));
        await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> countPutRequestsTo("/hearing/8bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f") == 1);

        MOCK_SERVER.verify(
                putRequestedFor(urlMatching("/hearing/8bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f"))
                        // Values from incoming case
                        .withRequestBody(matchingJsonPath("caseId", equalTo("D517D32D-3C80-41E8-846E-D274DC2B94A5")))
                        .withRequestBody(matchingJsonPath("hearingId", equalTo("8bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f")))
                        .withRequestBody(matchingJsonPath("hearingEventType", equalTo("Resulted")))
                        .withRequestBody(matchingJsonPath("caseMarkers[0].markerTypeDescription", equalTo("description 1")))
                        .withRequestBody(matchingJsonPath("caseMarkers[1].markerTypeDescription", equalTo("description 2")))
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
                        .withRequestBody(matchingJsonPath("defendants[0].offences[0].offenceCode", equalTo("ABC001")))
                        .withRequestBody(matchingJsonPath("defendants[0].offences[0].plea.pleaValue", equalTo("value 1")))
                        .withRequestBody(matchingJsonPath("defendants[0].offences[0].plea.pleaDate", equalTo("2021-09-08")))
                        .withRequestBody(matchingJsonPath("defendants[0].offences[0].verdict.verdictType.description", equalTo("description 1")))
                        .withRequestBody(matchingJsonPath("defendants[0].offences[0].verdict.verdictDate", equalTo("2021-09-08")))
                        .withRequestBody(matchingJsonPath("defendants[1].offences[1].listNo", equalTo("30")))
                        .withRequestBody(matchingJsonPath("defendants[0].offences[1].offenceCode", equalTo("ABC002")))
                        .withRequestBody(matchingJsonPath("defendants[1].phoneNumber.home", absent()))
                        .withRequestBody(matchingJsonPath("defendants[1].phoneNumber.work", equalTo("07000000005")))
                        .withRequestBody(matchingJsonPath("defendants[1].phoneNumber.mobile", equalTo("07000000006")))
        );

        
        verify(telemetryService).trackHearingMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackHearingChangedEvent(any(Hearing.class));
        verify(telemetryService, never()).trackOffenderMatchEvent(any(Defendant.class), any(Hearing.class), any(MatchResponse.class));
        verify(telemetryService, times(2)).trackDefendantProbationStatusUpdatedEvent(any(Defendant.class));
        verifyNoMoreInteractions(telemetryService);
    }



    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void givenNewCase_whenReceivePayload_thenSendNewCase(boolean matchOnEveryRecordUpdate) throws IOException {
        featureFlags.setFlagValue("match-on-every-no-record-update", matchOnEveryRecordUpdate);
        var hearing = Files.readString(Paths.get(BASE_PATH + "/common-platform/hearing.json"));
        publishMessage(hearing, Map.of("eventType",
            MessageAttributeValue.builder().dataType("String").stringValue(eventType).build(),
            "messageType", MessageAttributeValue.builder().dataType("String").stringValue("COMMON_PLATFORM_HEARING").build(), "hearingEventType", MessageAttributeValue.builder().dataType("String").stringValue("ConfirmedOrUpdated").build()));
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
                        .withRequestBody(matchingJsonPath("defendants[0].crn", equalTo("B123435")))
                        .withRequestBody(matchingJsonPath("defendants[1].crn", absent()))
                        .withRequestBody(matchingJsonPath("defendants[0].offences[0].listNo", equalTo("30")))
                        .withRequestBody(matchingJsonPath("defendants[0].offences[0].offenceCode", equalTo("ABC001")))
        );

        
        verify(telemetryService).trackHearingMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackNewHearingEvent(any(Hearing.class), any(String.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void givenNewCase_whenReceivePayloadForOrganisation_thenSendNewCase(boolean matchOnEveryRecordUpdate) throws IOException {
        featureFlags.setFlagValue("match-on-every-no-record-update", matchOnEveryRecordUpdate);
        var orgJson = Files.readString(Paths.get(BASE_PATH + "/common-platform/hearing-with-legal-entity-defendant.json"));
        publishMessage(orgJson, Map.of("messageType", MessageAttributeValue.builder().dataType("String").stringValue("COMMON_PLATFORM_HEARING").build(), "hearingEventType", MessageAttributeValue.builder().dataType("String").stringValue("ConfirmedOrUpdated").build()));

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
                        .withRequestBody(matchingJsonPath("defendants[0].crn", equalTo("B123435")))
                        .withRequestBody(matchingJsonPath("defendants[1].type", equalTo("ORGANISATION")))
                        .withRequestBody(matchingJsonPath("defendants[1].defendantId", equalTo("903c4c54-f667-4770-8fdf-1adbb5957c25")))
        );

        
        verify(telemetryService).trackHearingMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackNewHearingEvent(any(Hearing.class), any(String.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @Test
    public void givenNewCase_whenExactPersonRecordFound_thenSetPersonIdOnDefendant() throws IOException {
        featureFlags.setFlagValue("save_person_id_to_court_case_service", true);
        var orgJson = Files.readString(Paths.get(BASE_PATH + "/common-platform/hearing-with-legal-entity-defendant.json"));

        publishMessage(orgJson, Map.of("messageType", MessageAttributeValue.builder().dataType("String").stringValue("COMMON_PLATFORM_HEARING").build(), "hearingEventType", MessageAttributeValue.builder().dataType("String").stringValue("ConfirmedOrUpdated").build()));
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
                        .withRequestBody(matchingJsonPath("defendants[0].personId", absent()))

                        .withRequestBody(matchingJsonPath("defendants[0].crn", equalTo("B123435")))
                        .withRequestBody(matchingJsonPath("defendants[1].type", equalTo("ORGANISATION")))
                        .withRequestBody(matchingJsonPath("defendants[1].defendantId", equalTo("903c4c54-f667-4770-8fdf-1adbb5957c25")))
                        .withRequestBody(matchingJsonPath("defendants[1].personId", equalTo("e374e376-e2a3-11ed-b5ea-0242ac120002")))


        );

        
        verify(telemetryService).trackHearingMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackNewHearingEvent(any(Hearing.class), any(String.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @Test
    public void givenLargeHearing_whenExactPersonRecordFound_thenSetPersonIdOnDefendant() throws IOException {
        featureFlags.setFlagValue("save_person_id_to_court_case_service", true);
        var s3Key = UUID.randomUUID().toString();

        s3Client.putObject(builder -> builder.bucket(s3LargeHearingBucket).key(s3Key).build(), Paths.get(BASE_PATH + "/common-platform/hearing-with-legal-entity-defendant.json"));

        String messageBody = "[ \"software.amazon.payloadoffloading.PayloadS3Pointer\", {\n" +
            String.format("  \"s3BucketName\" : \"%s\",\n", s3LargeHearingBucket) +
            String.format("  \"s3Key\" : \"%s\"\n", s3Key) +
            "} ]";

        publishMessage(messageBody, Map.of(
            "eventType", MessageAttributeValue.builder().dataType("String").stringValue(largeEventType).build(),
            "messageType",
            MessageAttributeValue.builder().dataType("String").stringValue("COMMON_PLATFORM_HEARING").build(),
            "hearingEventType",
            MessageAttributeValue.builder().dataType("String").stringValue("ConfirmedOrUpdated").build(),
            "ExtendedPayloadSize",
            MessageAttributeValue.builder().dataType("String").stringValue("268444").build()
        ));
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
                .withRequestBody(matchingJsonPath("defendants[0].personId", absent()))

                .withRequestBody(matchingJsonPath("defendants[0].crn", equalTo("B123435")))
                .withRequestBody(matchingJsonPath("defendants[1].type", equalTo("ORGANISATION")))
                .withRequestBody(matchingJsonPath("defendants[1].defendantId", equalTo("903c4c54-f667-4770-8fdf-1adbb5957c25")))
                .withRequestBody(matchingJsonPath("defendants[1].personId", equalTo("e374e376-e2a3-11ed-b5ea-0242ac120002")))
        );


        verify(telemetryService).trackHearingMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackNewHearingEvent(any(Hearing.class), any(String.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void givenMatchedExistingCase_whenReceivePayload_thenSendUpdatedCase(boolean matchOnEveryRecordUpdate) throws IOException {
        featureFlags.setFlagValue("match-on-every-no-record-update", matchOnEveryRecordUpdate);
        var hearing = Files.readString(Paths.get(BASE_PATH + "/libra/case.json"));

        publishMessage(hearing, Map.of("messageType", MessageAttributeValue.builder().dataType("String").stringValue("LIBRA_COURT_CASE").build(), "hearingEventType", MessageAttributeValue.builder().dataType("String").stringValue("Resulted").build()));
        await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> countPutRequestsTo("/hearing/.*") == 1);

        MOCK_SERVER.verify(
                putRequestedFor(urlMatching("/hearing/.*"))
                        .withRequestBody(matchingJsonPath("defendants[0].pnc", equalTo("2004/0012345U")))
                        .withRequestBody(matchingJsonPath("hearingDays[0].listNo", equalTo("1st")))
                        .withRequestBody(matchingJsonPath("caseNo", equalTo("1600032981")))
                        .withRequestBody(matchingJsonPath("hearingDays[0].courtCode", equalTo("B10JQ")))
                        .withRequestBody(matchingJsonPath("defendants[0].probationStatus", equalTo("CURRENT")))
                        .withRequestBody(matchingJsonPath("defendants[0].cid", equalTo("CID1234")))
        );

        
        verify(telemetryService).trackHearingMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackHearingChangedEvent(any(Hearing.class));
        verify(telemetryService).trackDefendantProbationStatusUpdatedEvent(any(Defendant.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void givenExistingCase_whenReceivePayloadForOrganisation_thenSendUpdatedCase(boolean matchOnEveryRecordUpdate) throws IOException {
        featureFlags.setFlagValue("match-on-every-no-record-update", matchOnEveryRecordUpdate);

        var orgJson = Files.readString(Paths.get(BASE_PATH + "/libra/case-org.json"));

        publishMessage(orgJson, Map.of("messageType", MessageAttributeValue.builder().dataType("String").stringValue("LIBRA_COURT_CASE").build(), "hearingEventType", MessageAttributeValue.builder().dataType("String").stringValue("ConfirmedOrUpdated").build()));

        final var expectedEndpoint = String.format("/hearing/%s", "A0884637-5A70-4622-88E9-7324949B8E7A");
        await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> countPutRequestsTo(expectedEndpoint) == 1);

        MOCK_SERVER.verify(
                putRequestedFor(urlMatching(expectedEndpoint))
                        .withRequestBody(matchingJsonPath("caseId", equalTo("A0884637-5A70-4622-88E9-7324949B8E7A")))
                        .withRequestBody(matchingJsonPath("hearingId", equalTo("A0884637-5A70-4622-88E9-7324949B8E7A")))
                        .withRequestBody(matchingJsonPath("hearingDays[0].courtRoom", equalTo("07")))
                        .withRequestBody(matchingJsonPath("defendants[0].type", equalTo("ORGANISATION")))
                        .withRequestBody(matchingJsonPath("defendants[0].defendantId", equalTo("51EB661C-6CDF-46B2-ACF3-95098CF41154")))
                        .withRequestBody(matchingJsonPath("defendants[0].cid", equalTo("CID1234567")))
        );

        
        verify(telemetryService).trackHearingMessageReceivedEvent(any(String.class));
        verify(telemetryService).trackHearingChangedEvent(any(Hearing.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @Test
    public void givenNewCase_whenReceivePayload_thenYouthCasesNotProcessed() throws IOException {
        featureFlags.setFlagValue("match-on-every-no-record-update", true);
        var hearing = Files.readString(Paths.get(BASE_PATH + "/common-platform/youthDefendantHearing.json"));

        publishMessage(hearing, Map.of("messageType", MessageAttributeValue.builder().dataType("String").stringValue("COMMON_PLATFORM_HEARING").build(), "hearingEventType", MessageAttributeValue.builder().dataType("String").stringValue("ConfirmedOrUpdated").build()));

        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> countPutRequestsTo("/hearing/E10E3EF3-8637-40E3-BDED-8ED104A380AC") == 0);

        
        verify(telemetryService).trackHearingMessageReceivedEvent(any(String.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @Test
    public void givenNewHearing_with_multiple_cases_whenReceivePayload_thenProcessed() throws IOException {
        featureFlags.setFlagValue("match-on-every-no-record-update", false);
        var hearing = Files.readString(Paths.get(BASE_PATH + "/common-platform/hearing-multiple-cases.json"));

        publishMessage(hearing,
            Map.of("messageType", MessageAttributeValue.builder().dataType("String").stringValue("COMMON_PLATFORM_HEARING").build(),
                "hearingEventType", MessageAttributeValue.builder().dataType("String").stringValue("ConfirmedOrUpdated").build()));

        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> countPutRequestsTo("/hearing/E10E3EF3-8637-40E3-BDED-8ED104A380AC") == 2);

        verify(telemetryService).trackHearingMessageReceivedEvent(any(String.class));
        verify(telemetryService, times(2)).trackOffenderMatchEvent(any(Defendant.class), any(Hearing.class), any(MatchResponse.class));
        verify(telemetryService, times(2)).trackNewHearingEvent(any(Hearing.class), any(String.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @Test
    public void givenExistingHearing_with_multiple_cases_whenReceivePayload_thenProcessed() throws IOException {
        featureFlags.setFlagValue("match-on-every-no-record-update", true);
        var hearing = Files.readString(Paths.get(BASE_PATH + "/common-platform/youthDefendantHearing.json"));

        publishMessage(hearing,
            Map.of("messageType", MessageAttributeValue.builder().dataType("String").stringValue("COMMON_PLATFORM_HEARING").build(),
                "hearingEventType", MessageAttributeValue.builder().dataType("String").stringValue("ConfirmedOrUpdated").build()));

        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> countPutRequestsTo("/hearing/E10E3EF3-8637-40E3-BDED-8ED104A380AC") == 0);

        verify(telemetryService).trackHearingMessageReceivedEvent(any(String.class));
        verifyNoMoreInteractions(telemetryService);
    }

    @TestConfiguration
    public static class AwsTestConfig {

        @MockBean
        private TelemetryService telemetryService;
        @Autowired
        @Qualifier("hearingProcessor")
        private HearingProcessor caseMessageProcessor;
        @Autowired
        private HearingExtractor caseExtractor;

        @Bean
        public SqsMessageReceiver sqsMessageReceiver() {
            return new SqsMessageReceiver(caseMessageProcessor, telemetryService, caseExtractor);
        }
    }

    private void publishMessage(String hearing, Map<String, MessageAttributeValue> attributes) {
        HmppsTopicKt.publish(topic, eventType, hearing,true, attributes, DEFAULT_RETRY_POLICY, DEFAULT_BACKOFF_POLICY, "COURT_HEARING_EVENT_RECEIVER");
    }

    public int countPutRequestsTo(final String url) {
        return MOCK_SERVER.findAll(putRequestedFor(urlMatching(url))).size();
    }

}
