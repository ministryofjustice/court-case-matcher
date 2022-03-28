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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.repository.CourtCaseRepository;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;
import uk.gov.justice.probation.courtcasematcher.service.SqsAdminService;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.probation.courtcasematcher.pact.DomainDataHelper.CASE_ID;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
@ExtendWith({PactConsumerTestExt.class})
@PactTestFor(providerName = "court-case-service", port = "8090")
@PactDirectory(value = "build/pacts")
class CourtCaseRestClientPactTest {

    private static final String BASE_MOCK_PATH = "src/test/resources/mocks/__files/";

    @Autowired
    private CourtCaseRestClient restClient;

    @Pact(provider="court-case-service", consumer="court-case-matcher")
    public V4Pact getCourtCaseByIdPact(PactDslWithProvider builder) throws IOException {

        String body = FileUtils.readFileToString(new File(BASE_MOCK_PATH + "get-court-case/GET_court_case_response_D517D32D-3C80-41E8-846E-D274DC2B94A5.json"), UTF_8);

        return builder
                .given("a case exists for caseId D517D32D-3C80-41E8-846E-D274DC2B94A5")
                .uponReceiving("a request for a case by case number")
                .path("/case/D517D32D-3C80-41E8-846E-D274DC2B94A5/extended")
                .method("GET")
                .willRespondWith()
                .headers(Map.of("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .body(body)
                .status(200)
                .toPact(V4Pact.class);
    }

    @Pact(provider="court-case-service", consumer="court-case-matcher")
    public V4Pact putMinimalCourtCaseByIdPact(PactDslWithProvider builder) {

        final var body = newJsonBody((rootObject) -> {
            rootObject.stringType("caseId");
            rootObject.stringValue("source", "COMMON_PLATFORM");
            rootObject.array("defendants", (defendants) -> defendants.object((defendant -> {
                defendant.object("address", (addressObj -> {
                    addressObj.stringType("line1");
                }));
                defendant.date("dateOfBirth","yyyy-MM-dd");
                defendant.object("name", (name -> {
                    name.stringType("title");
                    name.stringType("forename1");
                    name.stringType("surname");
                }));
                defendant.array("offences",  (offences)-> offences.object((offence) -> {
                    offence.stringType("offenceTitle");
                    offence.stringType("offenceSummary");
                    offence.stringType("act");
                    offence.integerType("sequenceNumber");
                }));
                defendant.stringType("sex");
                defendant.stringValue("type", "PERSON");
                defendant.stringType("defendantId");
            })));
            rootObject.array("hearingDays", (array) -> array.object((hearingDay) -> {
                hearingDay.stringType("courtCode");
                hearingDay.stringType("courtRoom");
                hearingDay.stringType("listNo");
                hearingDay.datetime("sessionStartTime", "yyyy-MM-dd'T'HH:mm:ss");
            }));
        }).build();

        return builder
                .given("a case will be PUT by id")
                .uponReceiving("a request to put a minimal court case")
                .path(String.format("/case/%s/extended", CASE_ID))
                .headers("Content-type", "application/json")
                .method("PUT")
                .body(body)
                .willRespondWith()
                .status(201)
                .toPact(V4Pact.class);
    }

    @Pact(provider="court-case-service", consumer="court-case-matcher")
    public V4Pact putCourtCaseWithAllFieldsByIdPact(PactDslWithProvider builder) {

        final var body = newJsonBody((rootObject) -> {
            rootObject.stringType("caseId");
            rootObject.stringType("hearingId");
            rootObject.stringType("caseNo");
            rootObject.stringValue("source", "COMMON_PLATFORM");
            rootObject.array("defendants", (defendants) -> defendants.object((defendant -> {
                defendant.object("address", (addressObj -> {
                    addressObj.stringType("line1");
                    addressObj.stringType("line2");
                    addressObj.stringType("line3");
                    addressObj.stringType("line4");
                    addressObj.stringType("line5");
                    addressObj.stringType("postcode");
                }));
                defendant.date("dateOfBirth","yyyy-MM-dd");
                defendant.object("name", (name -> {
                    name.stringType("title");
                    name.stringType("forename1");
                    name.stringType("forename2");
                    name.stringType("forename3");
                    name.stringType("surname");
                }));
                defendant.array("offences",  (offences)-> offences.object((offence) -> {
                    offence.stringType("offenceTitle");
                    offence.stringType("offenceSummary");
                    offence.stringType("act");
                    offence.integerType("sequenceNumber");
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
            })));
            rootObject.array("hearingDays", (array) -> array.object((hearingDay) -> {
                hearingDay.stringType("courtCode");
                hearingDay.stringType("courtRoom");
                hearingDay.stringType("listNo");
                hearingDay.datetime("sessionStartTime", "yyyy-MM-dd'T'HH:mm:ss");
            }));
        }).build();

        final var location = String.format("/case/%s/extended", CASE_ID);
        return builder
                .given("a case will be PUT by id")
                .uponReceiving("a request to put a full court case")
                .body(body)
                .headers("Content-type", "application/json")
                .path(location)
                .method("PUT")
                .willRespondWith()
                .status(201)
                .toPact(V4Pact.class);
    }

    @PactTestFor(pactMethod = "getCourtCaseByIdPact")
    @Test
    void getCourtCaseById() {
        final var courtCase = restClient.getCourtCase(DomainDataHelper.aMinimalValidCourtCase().getCaseId()).block();
        assertThat(courtCase.getCaseId()).isEqualTo("D517D32D-3C80-41E8-846E-D274DC2B94A5");
    }

    @PactTestFor(pactMethod = "putMinimalCourtCaseByIdPact")
    @Test
    void putMinimalCourtCase() {
        final var actual = restClient.putCourtCase(DomainDataHelper.aMinimalValidCourtCase()).blockOptional();
        assertThat(actual).isEmpty();
    }

    @PactTestFor(pactMethod = "putCourtCaseWithAllFieldsByIdPact")
    @Test
    void putCourtCaseWithAllFields() {

        final var actual = ((CourtCaseRepository) restClient)
                .putCourtCase(DomainDataHelper.aCourtCaseWithAllFields()).blockOptional();

        assertThat(actual).isEmpty();
    }
}
