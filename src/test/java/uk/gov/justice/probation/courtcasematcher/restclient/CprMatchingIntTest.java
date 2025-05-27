package uk.gov.justice.probation.courtcasematcher.restclient;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import uk.gov.justice.hmpps.sqs.HmppsQueue;
import uk.gov.justice.hmpps.sqs.HmppsQueueService;
import uk.gov.justice.hmpps.sqs.HmppsTopic;
import uk.gov.justice.hmpps.sqs.HmppsTopicKt;
import uk.gov.justice.probation.courtcasematcher.application.FeatureFlags;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
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
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
public class CprMatchingIntTest {
    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);
    private static final String BASE_PATH = "src/test/resources/messages";

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @Autowired
    private FeatureFlags featureFlags;

    @Autowired
    private HmppsQueueService hmppsQueueService;

    HmppsTopic topic;

    @Value("${commonplatform.event.type.default}")
    private String eventType;

    RetryPolicy DEFAULT_RETRY_POLICY = new SimpleRetryPolicy();
    BackOffPolicy DEFAULT_BACKOFF_POLICY = new ExponentialBackOffPolicy();

    private static final String TOPIC_NAME = "courtcasestopic";

    @BeforeAll
    static void beforeAll() {
        MOCK_SERVER.start();
    }

    @AfterAll
    static void afterAll() {
        MOCK_SERVER.stop();
    }

    @BeforeEach
    void setUp(){
        topic = hmppsQueueService.findByTopicId(TOPIC_NAME);
        HmppsQueue queue = hmppsQueueService.findByQueueId("courtcasesqueue");

        queue.getSqsClient().purgeQueue(PurgeQueueRequest.builder().queueUrl(queue.getQueueUrl()).build());
        queue.getSqsDlqClient().purgeQueue(PurgeQueueRequest.builder().queueUrl(queue.getDlqUrl()).build());
    }

    @Test
    public void givenCprMatchesDefendant_and_has_no_CRN_updates_defendant_in_hearing() throws IOException {
        featureFlags.setFlagValue("match-on-every-no-record-update", false);
        var hearing = Files.readString(Paths.get(BASE_PATH + "/common-platform/cpr/hearing-one-defendant-no-crn.json"));

        publishMessage(hearing, Map.of("messageType", MessageAttributeValue.builder().dataType("String").stringValue("COMMON_PLATFORM_HEARING").build(), "hearingEventType", MessageAttributeValue.builder().dataType("String").stringValue("Resulted").build()));

        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> countPutRequestsTo("/hearing/b98fba88-1783-4bbb-8bf2-268caecf4b94") == 1);

        MOCK_SERVER.verify(
            putRequestedFor(urlMatching("/hearing/b98fba88-1783-4bbb-8bf2-268caecf4b94"))
                .withRequestBody(matchingJsonPath("caseId", equalTo("9f0ca828-477e-4f53-ba80-ce9f29897ee7")))
                .withRequestBody(matchingJsonPath("hearingId", equalTo("b98fba88-1783-4bbb-8bf2-268caecf4b94")))
                .withRequestBody(matchingJsonPath("hearingEventType", equalTo("Resulted")))
                .withRequestBody(matchingJsonPath("urn", equalTo("82CD34397719")))
                .withRequestBody(matchingJsonPath("caseNo", equalTo("9f0ca828-477e-4f53-ba80-ce9f29897ee7")))
                .withRequestBody(matchingJsonPath("hearingDays[0].courtCode", equalTo("B43KB")))
                .withRequestBody(matchingJsonPath("defendants[0].defendantId", equalTo("dfaa9be1-8daf-480b-b80a-b4dda1da3bef")))
                .withRequestBody(matchingJsonPath("defendants[0].crn", absent()))
                .withRequestBody(matchingJsonPath("defendants[0].cprUUID", equalTo("f264bdf5-56cf-45ff-9371-470b18f5c6cb")))
                .withRequestBody(matchingJsonPath("defendants[0].phoneNumber.home", equalTo("+44 114 496 2345")))
                .withRequestBody(matchingJsonPath("defendants[0].phoneNumber.work", equalTo("0114 496 0000")))
                .withRequestBody(matchingJsonPath("defendants[0].phoneNumber.mobile", equalTo("555 CRIME")))
                .withRequestBody(matchingJsonPath("defendants[0].probationStatus", equalTo("NO_RECORD")))
                .withRequestBody(matchingJsonPath("defendants[0].breach", absent()))
                .withRequestBody(matchingJsonPath("defendants[0].awaitingPsr", absent()))
                .withRequestBody(matchingJsonPath("defendants[0].offences[0].listNo", equalTo("5")))
                .withRequestBody(matchingJsonPath("defendants[0].offences[0].offenceCode", equalTo("OF61102")))
                .withRequestBody(matchingJsonPath("defendants[0].offences[0].plea", absent()))
                .withRequestBody(matchingJsonPath("defendants[0].offences[0].verdict",  absent()))
                .withRequestBody(matchingJsonPath("defendants[0].offences[1].listNo", equalTo("7")))
                .withRequestBody(matchingJsonPath("defendants[0].offences[1].offenceCode", equalTo("OF61102")))
                .withRequestBody(matchingJsonPath("defendants[0].offences[1].plea", absent()))
                .withRequestBody(matchingJsonPath("defendants[0].offences[1].verdict",  absent()))
                .withRequestBody(matchingJsonPath("defendants[0].address.line1",  equalTo("13 Wind Street")))
                .withRequestBody(matchingJsonPath("defendants[0].address.line2",  equalTo("Swansea")))
                .withRequestBody(matchingJsonPath("defendants[0].address.line3",  equalTo("Wales")))
                .withRequestBody(matchingJsonPath("defendants[0].address.line4",  equalTo("UK")))
                .withRequestBody(matchingJsonPath("defendants[0].address.line5",  equalTo("Earth")))
                .withRequestBody(matchingJsonPath("defendants[0].address.postcode",  equalTo("SA1 1FU")))
                .withRequestBody(matchingJsonPath("defendants[0].dateOfBirth",  equalTo("1982-12-01")))
                .withRequestBody(matchingJsonPath("defendants[0].name.forename1",  equalTo("Jane")))
                .withRequestBody(matchingJsonPath("defendants[0].name.surname",  equalTo("Doe")))
        );

        MOCK_SERVER.checkForUnmatchedRequests();
    }

    @Test
    public void givenCprMatchesDefendant_and_has_one_CRN_updates_defendant_in_hearing() throws IOException {
        featureFlags.setFlagValue("match-on-every-no-record-update", false);
        var hearing = Files.readString(Paths.get(BASE_PATH + "/common-platform/cpr/hearing-one-defendant-one-crn.json"));

        publishMessage(hearing, Map.of("messageType", MessageAttributeValue.builder().dataType("String").stringValue("COMMON_PLATFORM_HEARING").build(), "hearingEventType", MessageAttributeValue.builder().dataType("String").stringValue("Resulted").build()));

        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> countPutRequestsTo("/hearing/2f3b08ea-fd69-465a-b4b3-ffda12093fba") == 1);

        MOCK_SERVER.verify(
            putRequestedFor(urlMatching("/hearing/2f3b08ea-fd69-465a-b4b3-ffda12093fba"))
                .withRequestBody(matchingJsonPath("caseId", equalTo("5ab29194-687d-4260-9c43-b32e6f4b75db")))
                .withRequestBody(matchingJsonPath("hearingId", equalTo("2f3b08ea-fd69-465a-b4b3-ffda12093fba")))
                .withRequestBody(matchingJsonPath("hearingEventType", equalTo("Resulted")))
                .withRequestBody(matchingJsonPath("urn", equalTo("82CD34397719")))
                .withRequestBody(matchingJsonPath("caseNo", equalTo("5ab29194-687d-4260-9c43-b32e6f4b75db")))
                .withRequestBody(matchingJsonPath("hearingDays[0].courtCode", equalTo("B43KB")))
                .withRequestBody(matchingJsonPath("defendants[0].defendantId", equalTo("f64f8b81-a459-4588-9ea5-3be375304ce9")))
                .withRequestBody(matchingJsonPath("defendants[0].crn", equalTo("V147283")))
                .withRequestBody(matchingJsonPath("defendants[0].cprUUID", equalTo("84e022e0-773d-4a36-9829-03c76bcaa789")))
                .withRequestBody(matchingJsonPath("defendants[0].phoneNumber.home", equalTo("+44 114 496 2345")))
                .withRequestBody(matchingJsonPath("defendants[0].phoneNumber.work", equalTo("0114 496 0000")))
                .withRequestBody(matchingJsonPath("defendants[0].phoneNumber.mobile", equalTo("555 CRIME")))
                .withRequestBody(matchingJsonPath("defendants[0].probationStatus", equalTo("CURRENT")))
                .withRequestBody(matchingJsonPath("defendants[0].breach", absent()))
                .withRequestBody(matchingJsonPath("defendants[0].awaitingPsr", absent()))
                .withRequestBody(matchingJsonPath("defendants[0].offences[0].listNo", equalTo("5")))
                .withRequestBody(matchingJsonPath("defendants[0].offences[0].offenceCode", equalTo("OF61102")))
                .withRequestBody(matchingJsonPath("defendants[0].offences[0].plea", absent()))
                .withRequestBody(matchingJsonPath("defendants[0].offences[0].verdict",  absent()))
                .withRequestBody(matchingJsonPath("defendants[0].offences[1].listNo", equalTo("7")))
                .withRequestBody(matchingJsonPath("defendants[0].offences[1].offenceCode", equalTo("OF61102")))
                .withRequestBody(matchingJsonPath("defendants[0].offences[1].plea", absent()))
                .withRequestBody(matchingJsonPath("defendants[0].offences[1].verdict",  absent()))
                .withRequestBody(matchingJsonPath("defendants[0].address.line1",  equalTo("1 West Street")))
                .withRequestBody(matchingJsonPath("defendants[0].address.line2",  equalTo("Sheffield")))
                .withRequestBody(matchingJsonPath("defendants[0].address.line3",  equalTo("England")))
                .withRequestBody(matchingJsonPath("defendants[0].address.line4",  equalTo("UK")))
                .withRequestBody(matchingJsonPath("defendants[0].address.line5",  equalTo("Sheffield")))
                .withRequestBody(matchingJsonPath("defendants[0].address.postcode",  equalTo("SA4 1FU")))
                .withRequestBody(matchingJsonPath("defendants[0].dateOfBirth",  equalTo("1983-06-01")))
                .withRequestBody(matchingJsonPath("defendants[0].name.forename1",  equalTo("Bob")))
                .withRequestBody(matchingJsonPath("defendants[0].name.surname",  equalTo("Dole")))
        );

        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> countPostRequestsTo("/defendant/f64f8b81-a459-4588-9ea5-3be375304ce9/grouped-offender-matches") == 1);

        MOCK_SERVER.verify(
            postRequestedFor(urlMatching("/defendant/f64f8b81-a459-4588-9ea5-3be375304ce9/grouped-offender-matches"))
                .withRequestBody(matchingJsonPath("matches[0].matchIdentifiers.crn",  equalTo("V147283")))
        );

        MOCK_SERVER.checkForUnmatchedRequests();
    }

    @Test
    public void givenCprMatchesDefendant_and_has_multiple_CRN_updates_defendant_in_hearing() throws IOException {
        featureFlags.setFlagValue("match-on-every-no-record-update", false);
        var hearing = Files.readString(Paths.get(BASE_PATH + "/common-platform/cpr/hearing-one-defendant-multiple-crn.json"));

        publishMessage(hearing, Map.of("messageType", MessageAttributeValue.builder().dataType("String").stringValue("COMMON_PLATFORM_HEARING").build(), "hearingEventType", MessageAttributeValue.builder().dataType("String").stringValue("Resulted").build()));

        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> countPutRequestsTo("/hearing/0c4539d9-7acc-4637-9a2d-b6cc567c9cf2") == 1);

        MOCK_SERVER.verify(
            putRequestedFor(urlMatching("/hearing/0c4539d9-7acc-4637-9a2d-b6cc567c9cf2"))
                .withRequestBody(matchingJsonPath("caseId", equalTo("bf999b5f-e5ce-47b5-b5d2-7d991b9e14d1")))
                .withRequestBody(matchingJsonPath("hearingId", equalTo("0c4539d9-7acc-4637-9a2d-b6cc567c9cf2")))
                .withRequestBody(matchingJsonPath("hearingEventType", equalTo("Resulted")))
                .withRequestBody(matchingJsonPath("urn", equalTo("82CD34397719")))
                .withRequestBody(matchingJsonPath("caseNo", equalTo("bf999b5f-e5ce-47b5-b5d2-7d991b9e14d1")))
                .withRequestBody(matchingJsonPath("hearingDays[0].courtCode", equalTo("B43KB")))
                .withRequestBody(matchingJsonPath("defendants[0].defendantId", equalTo("48eae4d0-cdd7-43d8-9ac8-e328b033516f")))
                .withRequestBody(matchingJsonPath("defendants[0].crn", absent()))
                .withRequestBody(matchingJsonPath("defendants[0].cprUUID", equalTo("1e123672-3427-4d9e-968b-f7e854672074")))
                .withRequestBody(matchingJsonPath("defendants[0].phoneNumber.home", equalTo("+44 114 496 2345")))
                .withRequestBody(matchingJsonPath("defendants[0].phoneNumber.work", equalTo("0114 496 0000")))
                .withRequestBody(matchingJsonPath("defendants[0].phoneNumber.mobile", equalTo("555 CRIME")))
                .withRequestBody(matchingJsonPath("defendants[0].probationStatus", equalTo("NO_RECORD")))
                .withRequestBody(matchingJsonPath("defendants[0].breach", absent()))
                .withRequestBody(matchingJsonPath("defendants[0].awaitingPsr", absent()))
                .withRequestBody(matchingJsonPath("defendants[0].offences[0].listNo", equalTo("5")))
                .withRequestBody(matchingJsonPath("defendants[0].offences[0].offenceCode", equalTo("OF61102")))
                .withRequestBody(matchingJsonPath("defendants[0].offences[0].plea", absent()))
                .withRequestBody(matchingJsonPath("defendants[0].offences[0].verdict",  absent()))
                .withRequestBody(matchingJsonPath("defendants[0].offences[1].listNo", equalTo("7")))
                .withRequestBody(matchingJsonPath("defendants[0].offences[1].offenceCode", equalTo("OF61102")))
                .withRequestBody(matchingJsonPath("defendants[0].offences[1].plea", absent()))
                .withRequestBody(matchingJsonPath("defendants[0].offences[1].verdict",  absent()))
                .withRequestBody(matchingJsonPath("defendants[0].address.line1",  equalTo("1 West Street")))
                .withRequestBody(matchingJsonPath("defendants[0].address.line2",  equalTo("Sheffield")))
                .withRequestBody(matchingJsonPath("defendants[0].address.line3",  equalTo("England")))
                .withRequestBody(matchingJsonPath("defendants[0].address.line4",  equalTo("UK")))
                .withRequestBody(matchingJsonPath("defendants[0].address.line5",  equalTo("Sheffield")))
                .withRequestBody(matchingJsonPath("defendants[0].address.postcode",  equalTo("SA4 1FU")))
                .withRequestBody(matchingJsonPath("defendants[0].dateOfBirth",  equalTo("1983-06-01")))
                .withRequestBody(matchingJsonPath("defendants[0].name.forename1",  equalTo("Bob")))
                .withRequestBody(matchingJsonPath("defendants[0].name.surname",  equalTo("Dole")))
        );

        await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> countPostRequestsTo("/defendant/48eae4d0-cdd7-43d8-9ac8-e328b033516f/grouped-offender-matches") == 1);

        MOCK_SERVER.verify(
            postRequestedFor(urlMatching("/defendant/48eae4d0-cdd7-43d8-9ac8-e328b033516f/grouped-offender-matches"))
                .withRequestBody(matchingJsonPath("matches[0].matchIdentifiers.crn",  equalTo("V147283")))
                .withRequestBody(matchingJsonPath("matches[1].matchIdentifiers.crn",  equalTo("E158374")))
        );

        MOCK_SERVER.checkForUnmatchedRequests();
    }

    private void publishMessage(String hearing, Map<String, MessageAttributeValue> attributes) {
        HmppsTopicKt.publish(topic, eventType, hearing,true, attributes, DEFAULT_RETRY_POLICY, DEFAULT_BACKOFF_POLICY, "COURT_HEARING_EVENT_RECEIVER");
    }

    public int countPutRequestsTo(final String url) {
        return MOCK_SERVER.findAll(putRequestedFor(urlMatching(url))).size();
    }

    public int countPostRequestsTo(final String url) {
        return MOCK_SERVER.findAll(postRequestedFor(urlMatching(url))).size();
    }
}
