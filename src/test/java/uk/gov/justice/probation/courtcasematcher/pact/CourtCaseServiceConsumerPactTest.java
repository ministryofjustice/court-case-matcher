package uk.gov.justice.probation.courtcasematcher.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.fluent.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "court-case-service")
@PactDirectory(value = "build/pacts")
class CourtCaseServiceConsumerPactTest {

    private static final String BASE_MOCK_PATH = "src/test/resources/mocks/__files/";

    @Pact(provider="court-case-service", consumer="court-case-matcher")
    public RequestResponsePact getCourtCasePact(PactDslWithProvider builder) throws IOException {

        String body = FileUtils.readFileToString(new File(BASE_MOCK_PATH + "get-court-case/GET_court_case_response_1600028913.json"), UTF_8);

        return builder
            .given("a case exists for court B10JQ and case number 1600028913")
            .uponReceiving("a request for a case by case number")
            .path("/court/B10JQ/case/1600028913")
            .method("GET")
            .willRespondWith()
            .headers(Map.of("Content-Type", MediaType.APPLICATION_JSON_VALUE))
            .body(body)
            .status(200)
            .toPact();
    }


    @PactTestFor(pactMethod = "getCourtCasePact")
    @Test
    void getCourtCase(MockServer mockServer) throws IOException {
        var httpResponse = Request
            .Get(mockServer.getUrl() + "/court/B10JQ/case/1600028913")
            .execute()
            .returnResponse();

        assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(200);
    }
}
