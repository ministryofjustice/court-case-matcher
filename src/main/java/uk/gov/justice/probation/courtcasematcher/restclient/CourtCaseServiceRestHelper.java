package uk.gov.justice.probation.courtcasematcher.restclient;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.CCSCourtCase;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.CCSGroupedOffenderMatchesRequest;

import java.time.Duration;

import static uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient.EXCEPTION_RETRY_FILTER;

@Component
@Slf4j
public class CourtCaseServiceRestHelper {
    private final WebClient webClient;

    @Getter
    @Setter
    @Value("${court-case-service.max-retries:3}")
    private int maxRetries;

    @Getter
    @Setter
    @Value("${court-case-service.min-backoff-seconds:3}")
    private int minBackOffSeconds;

    @Value("${court-case-service.disable-authentication:false}")
    private Boolean disableAuthentication;

    @Autowired
    public CourtCaseServiceRestHelper(@Qualifier("courtCaseServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    WebClient.RequestHeadersSpec<?> putObject(String path, CCSCourtCase CCSCourtCase, Class<CCSCourtCase> type) {
        WebClient.RequestHeadersSpec<?> spec = webClient
                .put()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .body(Mono.just(CCSCourtCase), type)
                .accept(MediaType.APPLICATION_JSON);

        return addSpecAuthAttribute(spec, path);
    }

    WebClient.RequestHeadersSpec<?> addSpecAuthAttribute(WebClient.RequestHeadersSpec<?> spec, String path) {
        if (disableAuthentication) {
            return spec;
        }

        log.info(String.format("Authenticating with %s for call to %s", "offender-search-client", path));
        return spec.attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("offender-search-client"));
    }

    WebClient.RequestHeadersSpec<?> postObject(String path, CCSGroupedOffenderMatchesRequest request, Class<CCSGroupedOffenderMatchesRequest> elementClass, LegacyCourtCaseRestClient courtCaseRestClient) {
        WebClient.RequestHeadersSpec<?> spec = webClient
            .post()
            .uri(uriBuilder -> uriBuilder.path(path).build())
            .body(Mono.just(request), elementClass)
            .accept(MediaType.APPLICATION_JSON);

        return addSpecAuthAttribute(spec, path);
    }

    Mono<Void> logRetrySignal(Retry.RetrySignal retrySignal, String messageFormat, String initialMessageFormat, String courtCode, String caseNo, LegacyCourtCaseRestClient courtCaseRestClient) {
        if (retrySignal.totalRetries() > 0 ) {
            log.warn(String.format(messageFormat, caseNo, courtCode, retrySignal.totalRetries(), maxRetries));
        }
        else {
            log.warn(String.format(initialMessageFormat, caseNo, courtCode));
        }
        return Mono.empty();
    }

    RetryBackoffSpec buildRetrySpec(String courtCode, String caseNo, String errorMsgFormatRetryPutCase, String errorMsgFormatInitialCase, LegacyCourtCaseRestClient courtCaseRestClient) {
        return Retry.backoff(maxRetries, Duration.ofSeconds(minBackOffSeconds))
                .jitter(0.0d)
                .doAfterRetryAsync((retrySignal) -> logRetrySignal(retrySignal, errorMsgFormatRetryPutCase, errorMsgFormatInitialCase, courtCode, caseNo, courtCaseRestClient))
                .filter(EXCEPTION_RETRY_FILTER);
    }

    WebClient.RequestHeadersSpec<?> get(String path, LegacyCourtCaseRestClient courtCaseRestClient) {
        final WebClient.RequestHeadersSpec<?> spec = webClient
            .get()
            .uri(uriBuilder -> uriBuilder.path(path).build())
            .accept(MediaType.APPLICATION_JSON);

        return addSpecAuthAttribute(spec, path);
    }
}
