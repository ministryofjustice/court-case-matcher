package uk.gov.justice.probation.courtcasematcher.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import com.google.common.eventbus.EventBus;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import uk.gov.justice.probation.courtcasematcher.model.domain.Address;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({PactConsumerTestExt.class, MockitoExtension.class, SpringExtension.class})
@PactTestFor(providerName = "court-case-service")
@PactDirectory(value = "build/pacts")
class CourtCaseServiceClientPactTest {

    private static final String BASE_MOCK_PATH = "src/test/resources/mocks/__files/";
    private static String CASE_ID = "D517D32D-3C80-41E8-846E-D274DC2B94A5";

    @Mock
    private EventBus bus;



    @Pact(provider="court-case-service", consumer="court-case-matcher")
    public RequestResponsePact putCourtCaseByCourtCodeAndCaseNo(PactDslWithProvider builder) throws IOException {

        String body = FileUtils.readFileToString(new File(BASE_MOCK_PATH + "/put-court-case/POST_matches.json"), UTF_8);

        final var location = String.format("/court/B10JQ/case/1234567890", CASE_ID);
        return builder
                .uponReceiving("a request to put a minimal court case")
                .path(location)
                .method("PUT")
                .body(body)
                .willRespondWith()
                .status(201)
                .toPact();
    }

    @Pact(provider="court-case-service", consumer="court-case-matcher")
    public RequestResponsePact putMinimalCourtCaseByIdPact(PactDslWithProvider builder) throws IOException {

        String body = FileUtils.readFileToString(new File(BASE_MOCK_PATH + "/put-court-case/POST_matches.json"), UTF_8);

        final var location = String.format("/case/%s/extended", CASE_ID);
        return builder
                .uponReceiving("a request to put a minimal court case")
                .path(location)
                .method("PUT")
                .body(body)
                .willRespondWith()
                .status(201)
                .toPact();
    }

    @Pact(provider="court-case-service", consumer="court-case-matcher")
    public RequestResponsePact putCourtCaseWithAllFieldsByIdPact(PactDslWithProvider builder) throws IOException {

        String body = FileUtils.readFileToString(new File(BASE_MOCK_PATH + "/put-court-case/POST_matches.json"), UTF_8);

        final var location = String.format("/case/%s/extended", CASE_ID);
        return builder
                .uponReceiving("a request to put a full court case")
                .body(body)
                .path(location)
                .method("PUT")
                .willRespondWith()
                .status(201)
                .toPact();
    }

    @PactTestFor(pactMethod = "putMinimalCourtCaseByIdPact")
    @Test
    void putMinimalCourtCase(MockServer mockServer) {

        final var mono = getCourtCaseRestClient(mockServer)
                .putCourtCase("B10JQ", "1234567890", aMinimalValidCourtCase());

        assertThat(mono.blockOptional()).isEmpty();
    }

    @PactTestFor(pactMethod = "putCourtCaseWithAllFieldsByIdPact")
    @Test
    void putCourtCaseWithAllFields(MockServer mockServer) {

        final var mono = getCourtCaseRestClient(mockServer)
                .putCourtCase("B10JQ", "1234567890", aCourtCaseWithAllFields());

        assertThat(mono.blockOptional()).isEmpty();
    }

    private CourtCase aCourtCaseWithAllFields() {
        return aMinimalCourtCaseBuilder()
                .caseNo("case no")
                .probationStatusActual("Current")
                .crn("crn")
                .cro("cro")
                .pnc("pnc")
                .name(Name.builder()
                        .title("title")
                        .forename1("forename 1")
                        .forename2("forename 2")
                        .forename3("forename 3")
                        .surname("surname")
                        .build())
                .defendantName("defendant name")
                .defendantSex("defendant sex")
                .nationality1("nationality 1")
                .nationality2("nationality 2")
                .breach(true)
                .previouslyKnownTerminationDate(LocalDate.of(2021, 8,25))
                .suspendedSentenceOrder(true)
                .preSentenceActivity(true)
                .awaitingPsr(true)
                .defendantSex("sex")
                .build();
    }

    private CourtCaseRestClient getCourtCaseRestClient(MockServer mockServer) {
        return new CourtCaseRestClient(getWebClient(mockServer),
                bus,
                "/court/%s/case/%s/grouped-offender-matches",
                "/court/%s/case/%s",
                true);
    }

    private CourtCase aMinimalValidCourtCase() {
        return aMinimalCourtCaseBuilder()
                .build();
    }

    private CourtCase.CourtCaseBuilder aMinimalCourtCaseBuilder() {
        return CourtCase.builder()
                .caseId(CASE_ID)
                .courtCode("B10JQ")
                .courtRoom("ROOM 1")
                .sessionStartTime(LocalDateTime.of(2021, 8, 26, 9, 0))
                .probationStatus("Current")
                .offences(Collections.singletonList(Offence.builder()
                        .offenceTitle("offence title")
                        .offenceSummary("offence summary")
                        .act("offence act")
                        .sequenceNumber(1)
                        .build()))
                .name(Name.builder()
                        .title("title")
                        .forename1("forename1")
                        .surname("surname")
                        .build())
                .defendantAddress(Address.builder()
                        .line1("line1")
                        .build())
                .defendantDob(LocalDate.of(1986, 11, 28))
                .defendantType(DefendantType.PERSON)
                .listNo("1");
    }

    private WebClient getWebClient(MockServer mockServer) {
        return defaultWebClientBuilder()
                .baseUrl(mockServer.getUrl())
                .build();
    }

    private WebClient.Builder defaultWebClientBuilder() {
        HttpClient httpClient = HttpClient.create();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
}
