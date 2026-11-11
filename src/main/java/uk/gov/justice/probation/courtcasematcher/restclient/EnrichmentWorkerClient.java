package uk.gov.justice.probation.courtcasematcher.restclient;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse;

@Component
public class EnrichmentWorkerClient {
    private final WebClient webClient;

    @Value("${enrichment-worker.disable-authentication:false}")
    private Boolean disableAuthentication;

    public EnrichmentWorkerClient(@Qualifier("enrichmentWorkerWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<EnrichmentWorkerResponse> getCommonPlatformEnrichment(String defendantId) {
        return get("/enrichment/common-platform/{defendantId}", defendantId);
    }

    public Mono<EnrichmentWorkerResponse> getLibraEnrichment(String cId) {
        return get("/enrichment/libra/{cId}", cId);
    }

    private Mono<EnrichmentWorkerResponse> get(String path, String identifier) {
        WebClient.RequestHeadersSpec<?> request = webClient.get()
                .uri(path, identifier)
                .accept(MediaType.APPLICATION_JSON);

        if (!disableAuthentication) {
            request = request.attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction
                    .clientRegistrationId("enrichment-worker-client"));
        }

        return request.retrieve()
                .bodyToMono(EnrichmentWorkerResponse.class)
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "Enrichment worker returned an empty response for identifier " + identifier)));
    }
}
