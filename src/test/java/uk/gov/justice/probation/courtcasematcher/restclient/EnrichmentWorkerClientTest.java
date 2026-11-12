package uk.gov.justice.probation.courtcasematcher.restclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse.EnrichmentSourceType;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse.EnrichmentStatus;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class EnrichmentWorkerClientTest {
    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    private EnrichmentWorkerClient enrichmentWorkerClient;

    @BeforeEach
    void setUp() {
        enrichmentWorkerClient = new EnrichmentWorkerClient(
                WebClient.builder().baseUrl("http://localhost:8090").build());
        ReflectionTestUtils.setField(enrichmentWorkerClient, "disableAuthentication", true);
    }

    @Test
    void retrievesCommonPlatformEnrichment() {
        MOCK_SERVER.stubFor(get(urlEqualTo("/enrichment/common-platform/cp-id"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "sourceType": "COMMON_PLATFORM",
                                  "targetId": "cp-id",
                                  "status": "ENRICHED",
                                  "defendant": {
                                    "cprUUID": "cpr-id",
                                    "name": {"title": "Mr", "forename1": "Alex", "surname": "Smith"},
                                    "dateOfBirth": "1990-01-02",
                                    "address": {"line1": "Building", "postcode": "SW1A 1AA"}
                                  },
                                  "groupedOffenderMatches": {
                                    "matches": [{
                                      "matchIdentifiers": {"crn": "X123"},
                                      "matchType": "NAME_DOB_ALIAS",
                                      "confirmed": false,
                                      "rejected": false
                                    }]
                                  }
                                }
                                """)));

        var result = enrichmentWorkerClient.getCommonPlatformEnrichment("cp-id").block();

        assertThat(result.sourceType()).isEqualTo(EnrichmentSourceType.COMMON_PLATFORM);
        assertThat(result.targetId()).isEqualTo("cp-id");
        assertThat(result.status()).isEqualTo(EnrichmentStatus.ENRICHED);
        assertThat(result.defendant().cprUUID()).isEqualTo("cpr-id");
        assertThat(result.defendant().name().forename1()).isEqualTo("Alex");
        assertThat(result.defendant().address().postcode()).isEqualTo("SW1A 1AA");
        assertThat(result.groupedOffenderMatches().matches()).hasSize(1);
        MOCK_SERVER.verify(getRequestedFor(urlEqualTo("/enrichment/common-platform/cp-id")));
    }

    @Test
    void retrievesLibraEnrichmentByCid() {
        MOCK_SERVER.stubFor(get(urlEqualTo("/enrichment/libra/1234567"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "sourceType": "LIBRA",
                                  "targetId": "1234567",
                                  "status": "NO_MATCH"
                                }
                                """)));

        var result = enrichmentWorkerClient.getLibraEnrichment("1234567").block();

        assertThat(result.sourceType()).isEqualTo(EnrichmentSourceType.LIBRA);
        assertThat(result.status()).isEqualTo(EnrichmentStatus.NO_MATCH);
        MOCK_SERVER.verify(getRequestedFor(urlEqualTo("/enrichment/libra/1234567")));
    }
}
