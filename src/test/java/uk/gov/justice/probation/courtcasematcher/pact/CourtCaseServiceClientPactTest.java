package uk.gov.justice.probation.courtcasematcher.pact;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseServiceClient;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.probation.courtcasematcher.pact.DomainDataHelper.HEARING_ID;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
@ExtendWith({PactConsumerTestExt.class})
@PactTestFor(providerName = "court-case-service", port = "8090")
@PactDirectory(value = "build/pacts")
class CourtCaseServiceClientPactTest {

    private static final String BASE_MOCK_PATH = "src/test/resources/mocks/__files/";

    @Autowired
    private CourtCaseServiceClient restClient;

    @Pact(provider = "court-case-service", consumer = "court-case-matcher")
    public V4Pact getHearingByIdPact(PactDslWithProvider builder) throws IOException {

        String body = FileUtils.readFileToString(new File(BASE_MOCK_PATH + "get-court-case/GET_court_case_response_D517D32D-3C80-41E8-846E-D274DC2B94A5.json"), UTF_8.name());

        return builder
                .given("a hearing exists for hearingId 8bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f")
                .uponReceiving("a request for a case by hearingId")
                .path("/hearing/8bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f")
                .method("GET")
                .willRespondWith()
                .headers(Map.of("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .body(body)
                .status(200)
                .toPact(V4Pact.class);
    }


    @Pact(provider = "court-case-service", consumer = "court-case-matcher")
    public V4Pact getHearingByCaseNoPact(PactDslWithProvider builder) throws IOException {

        String body = FileUtils.readFileToString(new File(BASE_MOCK_PATH + "get-court-case/GET_court_case_response_1600028913.json"), UTF_8.name());

        return builder
                .given("a hearing exists for court B10JQ, case number 1600028913 and list number 2nd")
                .uponReceiving("a request for a case by case number")
                .path("/court/B10JQ/case/1600028913")
                .query("listNo=2nd")
                .method("GET")
                .willRespondWith()
                .headers(Map.of("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .body(body)
                .status(200)
                .toPact(V4Pact.class);
    }

    @Pact(provider = "court-case-service", consumer = "court-case-matcher")
    public V4Pact putMinimalHearingByIdPact(PactDslWithProvider builder) {

        final var body = newJsonBody((rootObject) -> {
            rootObject.stringType("caseId");
            rootObject.stringType("hearingId");
            rootObject.stringValue("source", "COMMON_PLATFORM");
            rootObject.array("defendants", (defendants) -> defendants.object((defendant -> {
                defendant.object("address", (addressObj -> {
                    addressObj.stringType("line1");
                }));
                defendant.date("dateOfBirth", "yyyy-MM-dd");
                defendant.object("name", (name -> {
                    name.stringType("title");
                    name.stringType("forename1");
                    name.stringType("surname");
                }));
                defendant.array("offences", (offences) -> offences.object((offence) -> {
                    offence.stringType("offenceTitle");
                    offence.stringType("offenceSummary");
                    offence.stringType("act");
                    offence.integerType("sequenceNumber");
                    offence.stringType("offenceCode");
                    offence.object("plea", (pleaObject -> {
                        pleaObject.stringType("value");
                        pleaObject.date("date");
                    }));
                    offence.object("verdict", (verdictObject -> {
                        verdictObject.stringType("typeDescription");
                        verdictObject.date("date");
                    }));
                    offence.array("judicialResults", (judicialResults) -> judicialResults.object((judicialResult) -> {
                        judicialResult.booleanType("isConvictedResult");
                        judicialResult.stringType("label");
                        judicialResult.stringType("judicialResultTypeId");
                        judicialResult.stringType("resultText");
                    }));
                }));
                defendant.stringType("sex");
                defendant.stringValue("type", "PERSON");
                defendant.stringType("defendantId");
                defendant.stringType("personId");
            })));
            rootObject.array("hearingDays", (array) -> array.object((hearingDay) -> {
                hearingDay.stringType("courtCode");
                hearingDay.stringType("courtRoom");
                hearingDay.stringType("listNo");
                hearingDay.datetime("sessionStartTime", "yyyy-MM-dd'T'HH:mm:ss");
            }));
        }).build();

        return builder
                .given("a hearing will be PUT by id")
                .uponReceiving("a request to put a minimal court case")
                .path(String.format("/hearing/%s", HEARING_ID))
                .headers("Content-type", "application/json")
                .method("PUT")
                .body(body)
                .willRespondWith()
                .status(201)
                .toPact(V4Pact.class);
    }

    @Pact(provider = "court-case-service", consumer = "court-case-matcher")
    public V4Pact putHearingWithAllFieldsByIdPact(PactDslWithProvider builder) {

        final var body = newJsonBody((rootObject) -> {
            rootObject.stringType("caseId");
            rootObject.stringType("hearingId");
            rootObject.stringType("hearingType");
            rootObject.stringType("hearingEventType");
            rootObject.stringType("caseNo");
            rootObject.stringType("urn");
            rootObject.stringValue("source", "COMMON_PLATFORM");
            rootObject.stringType("hearingEventType");
            rootObject.array("defendants", (defendants) -> defendants.object((defendant -> {
                defendant.object("address", (addressObj -> {
                    addressObj.stringType("line1");
                    addressObj.stringType("line2");
                    addressObj.stringType("line3");
                    addressObj.stringType("line4");
                    addressObj.stringType("line5");
                    addressObj.stringType("postcode");
                }));
                defendant.date("dateOfBirth", "yyyy-MM-dd");
                defendant.object("name", (name -> {
                    name.stringType("title");
                    name.stringType("forename1");
                    name.stringType("forename2");
                    name.stringType("forename3");
                    name.stringType("surname");
                }));
                defendant.array("offences", (offences) -> offences.object((offence) -> {
                    offence.stringType("offenceTitle");
                    offence.stringType("offenceSummary");
                    offence.stringType("act");
                    offence.integerType("sequenceNumber");
                    offence.stringType("offenceCode");
                    offence.object("plea", (pleaObject -> {
                        pleaObject.stringType("value");
                        pleaObject.date("date");
                    }));
                    offence.object("verdict", (verdictObject -> {
                        verdictObject.stringType("typeDescription");
                        verdictObject.date("date");
                    }));
                    offence.array("judicialResults", (judicialResults) -> judicialResults.object((judicialResult) -> {
                        judicialResult.booleanType("isConvictedResult");
                        judicialResult.stringType("label");
                        judicialResult.stringType("judicialResultTypeId");
                        judicialResult.stringType("resultText");
                    }));
                }));
                defendant.stringType("probationStatus", "CURRENT");
                defendant.stringType("type", "ORGANISATION");
                defendant.stringType("crn");
                defendant.stringType("cro");
                defendant.stringType("pnc");
                defendant.booleanType("preSentenceActivity");
                defendant.date("previouslyKnownTerminationDate", "yyyy-MM-dd");
                defendant.stringType("sex");
                defendant.booleanType("suspendedSentenceOrder");
                defendant.booleanType("awaitingPsr");
                defendant.booleanType("breach");
                defendant.stringType("defendantId");
                defendant.stringType("personId");
            })));
            rootObject.array("hearingDays", (array) -> array.object((hearingDay) -> {
                hearingDay.stringType("courtCode");
                hearingDay.stringType("courtRoom");
                hearingDay.stringType("listNo");
                hearingDay.datetime("sessionStartTime", "yyyy-MM-dd'T'HH:mm:ss");
            }));
        }).build();

        final var location = String.format("/hearing/%s", HEARING_ID);
        return builder
                .given("a hearing will be PUT by id")
                .uponReceiving("a request to put a full court case")
                .body(body)
                .headers("Content-type", "application/json")
                .path(location)
                .method("PUT")
                .willRespondWith()
                .status(201)
                .toPact(V4Pact.class);
    }

    @PactTestFor(pactMethod = "getHearingByIdPact")
    @Test
    void getHearingById() {
        final var actual = restClient.getHearing("8bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f").block();
        assertThat(actual.getHearingId()).isEqualTo("8bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f");
        assertThat(actual.getDefendants().get(0).getConfirmedOffender()).isEqualTo(true);
        assertThat(actual.getDefendants()).extracting(Defendant::getPersonId).contains("96624bb7-c64d-46d9-a427-813ec168f95a", "25429322-5e82-42dc-8005-858b5d082f80");
    }


    @PactTestFor(pactMethod = "getHearingByCaseNoPact")
    @Test
    void getHearingByCaseNo() {

        final var actual = restClient.getHearing("B10JQ", "1600028913", "2nd").block();

        assertThat(actual).isNotNull();
        assertThat(actual.getDefendants().get(0).getConfirmedOffender()).isEqualTo(true);
        assertThat(actual.getDefendants().get(0).getPersonId()).isEqualTo("96624bb7-c64d-46d9-a427-813ec168f95a");
    }

    @PactTestFor(pactMethod = "putMinimalHearingByIdPact")
    @Test
    void putMinimalHearing() {
        final var actual = restClient.putHearing(DomainDataHelper.aMinimalValidCourtCase()).blockOptional();
        assertThat(actual).isEmpty();
    }

    @PactTestFor(pactMethod = "putHearingWithAllFieldsByIdPact")
    @Test
    void putHearingWithAllFields() {

        final var actual = restClient
                .putHearing(DomainDataHelper.aHearingWithAllFields()).blockOptional();

        assertThat(actual).isEmpty();
    }
}
