package uk.gov.justice.probation.courtcasematcher.pact;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.repository.CourtCaseRepository;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.probation.courtcasematcher.pact.DomainDataHelper.CASE_ID;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
@ExtendWith({PactConsumerTestExt.class})
@PactTestFor(providerName = "court-case-service", port = "8090")
@PactDirectory(value = "build/pacts")
class CourtCaseRestClientPactTest {

    @Autowired
    private CourtCaseRestClient restClient;

    @Pact(provider="court-case-service", consumer="court-case-matcher")
    public RequestResponsePact putMinimalCourtCaseByIdPact(PactDslWithProvider builder) {

        final var body = newJsonBody((rootObject) -> {
            rootObject.stringType("caseId");
            rootObject.stringType("courtCode");
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
                defendant.stringValue("type", "PERSON");
                defendant.booleanType("preSentenceActivity");
                defendant.booleanType("awaitingPsr");
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
                .uponReceiving("a request to put a minimal court case")
                .path(String.format("/case/%s/extended", CASE_ID))
                .headers("Content-type", "application/json")
                .method("PUT")
                .body(body)
                .willRespondWith()
                .status(201)
                .toPact();
    }

    @Pact(provider="court-case-service", consumer="court-case-matcher")
    @Disabled
    public RequestResponsePact putCourtCaseWithAllFieldsByIdPact(PactDslWithProvider builder) {


        final var body = newJsonBody((rootObject) -> {
            rootObject.stringType("caseId");
            rootObject.stringType("caseNo");
            rootObject.stringType("courtCode");
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
                defendant.stringType("probationStatus");
                defendant.stringType("type", "PERSON");
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
                .uponReceiving("a request to put a full court case")
                .body(body)
                .headers("Content-type", "application/json")
                .path(location)
                .method("PUT")
                .willRespondWith()
                .status(201)
                .toPact();
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
