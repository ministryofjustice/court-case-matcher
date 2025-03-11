package uk.gov.justice.probation.courtcasematcher.restclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.restclient.exception.CprCanonicalRecordNotFoundException;
import uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice.CprDefendant;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class CprServiceClient {
    @Value("${cpr-service.disable-authentication:false}")
    private Boolean disableAuthentication;

    private final WebClient webClient;

    public CprServiceClient(@Qualifier("cprWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<CprDefendant> getCprCanonicalRecord(String cprUUID) {
        final String path = String.format("/search/person/%s", cprUUID);

        return get(path)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (clientResponse) -> handleGetError(clientResponse, cprUUID))
            .bodyToMono(CprDefendant.class)
            .onErrorResume(CprCanonicalRecordNotFoundException.class, (e) -> Mono.empty());
    }

    WebClient.RequestHeadersSpec<?> get(String path) {
        final WebClient.RequestHeadersSpec<?> spec = webClient
            .get()
            .uri(uriBuilder -> {
                uriBuilder.path(path);
                return uriBuilder.build();
            })
            .accept(MediaType.APPLICATION_JSON);

        return addSpecAuthAttribute(spec, path);
    }

    WebClient.RequestHeadersSpec<?> addSpecAuthAttribute(WebClient.RequestHeadersSpec<?> spec, String path) {
        if (disableAuthentication) {
            return spec;
        }
        return spec.attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("offender-search-client"));
    }

    private Mono<? extends Throwable> handleGetError(ClientResponse clientResponse, String cprUUID) {
        final HttpStatusCode httpStatusCode = clientResponse.statusCode();
        // This is expected for new hearings
        if (HttpStatus.NOT_FOUND.equals(httpStatusCode)) {
            log.info("Failed to get cpr canonical record for cprUUID {}", cprUUID);
            return Mono.error(new CprCanonicalRecordNotFoundException(cprUUID));
        }
        else if(HttpStatus.UNAUTHORIZED.equals(httpStatusCode) || HttpStatus.FORBIDDEN.equals(httpStatusCode)) {
            log.error("HTTP status {} to to GET the cpr canonical record from cpr service", httpStatusCode);
        }
        throw WebClientResponseException.create(httpStatusCode.value(),
            httpStatusCode.toString(),
            clientResponse.headers().asHttpHeaders(),
            clientResponse.toString().getBytes(),
            StandardCharsets.UTF_8);
    }
}
