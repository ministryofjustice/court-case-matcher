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
import uk.gov.justice.probation.courtcasematcher.repository.CourtCaseRepository;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseServiceRestHelper;
import uk.gov.justice.probation.courtcasematcher.restclient.LegacyCourtCaseRestClient;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({PactConsumerTestExt.class, MockitoExtension.class, SpringExtension.class})
@PactTestFor(providerName = "court-case-service")
@PactDirectory(value = "build/pacts")
class CourtCaseServiceClientPactTest {

    private static final String BASE_MOCK_PATH = "src/test/resources/mocks/__files/";

    @Mock
    private EventBus bus;

    @Pact(provider="court-case-service", consumer="court-case-matcher")
    public RequestResponsePact putCourtCaseByCourtCodeAndCaseNo(PactDslWithProvider builder) throws IOException {

        String body = FileUtils.readFileToString(new File(BASE_MOCK_PATH + "/put-court-case/POST_matches.json"), UTF_8);

        final var location = String.format("/court/B10JQ/case/%s", DomainDataHelper.CASE_ID);
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

        final var location = String.format("/case/%s/extended", DomainDataHelper.CASE_ID);
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

        final var location = String.format("/case/%s/extended", DomainDataHelper.CASE_ID);
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
                .putCourtCase(DomainDataHelper.aMinimalValidCourtCase());

        assertThat(mono.blockOptional()).isEmpty();
    }

    @PactTestFor(pactMethod = "putCourtCaseWithAllFieldsByIdPact")
    @Test
    void putCourtCaseWithAllFields(MockServer mockServer) {

        final var mono = getCourtCaseRestClient(mockServer)
                .putCourtCase(DomainDataHelper.aCourtCaseWithAllFields());

        assertThat(mono.blockOptional()).isEmpty();
    }

    private CourtCaseRepository getCourtCaseRestClient(MockServer mockServer) {
        final var restHelper = getRestHelper(mockServer);
        final var legacyCourtCaseRestClient = new LegacyCourtCaseRestClient(restHelper,
                bus,
                "/court/%s/case/%s/grouped-offender-matches",
                "/court/%s/case/%s"
        );

        return new CourtCaseRestClient(legacyCourtCaseRestClient, restHelper, "/case/%s/extended");
    }

    private CourtCaseServiceRestHelper getRestHelper(MockServer mockServer) {
        return new CourtCaseServiceRestHelper(getWebClient(mockServer));
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
