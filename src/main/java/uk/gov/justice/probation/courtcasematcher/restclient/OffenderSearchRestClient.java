package uk.gov.justice.probation.courtcasematcher.restclient;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.Retry.RetrySignal;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchRequest;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.SearchResponses;

import java.time.Duration;
import java.util.function.Predicate;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Component
@Slf4j
public class OffenderSearchRestClient {

    @Setter
    @Value("${offender-search.post-match-url}")
    private String postMatchUrl;

    @Setter
    @Value("${offender-search.post-search-url}")
    private String postSearchUrl;

    @Setter
    @Value("${offender-search.disable-authentication:false}")
    private Boolean disableAuthentication;

    @Setter
    @Value("${offender-search.max-retries:3}")
    private int maxRetries;

    @Setter
    @Value("${offender-search.min-backoff-seconds:5}")
    private int minBackOffSeconds;

    private final WebClient webClient;

    @Autowired
    public OffenderSearchRestClient(@Qualifier("offenderSearchWebClient") WebClient webClient) {
        super();
        this.webClient = webClient;
    }
    public Mono<MatchResponse> match(MatchRequest body){

        return post(postMatchUrl)
                .body(BodyInserters.fromPublisher(Mono.just(body), MatchRequest.class))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(MatchResponse.class)
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(minBackOffSeconds))
                                .jitter(0.0d)
                                .doAfterRetryAsync(this::logRetrySignal)
                                .filter(EXCEPTION_RETRY_FILTER))
                .onErrorResume(this::handleError)
            ;
    }

    public Mono<SearchResponses> search(String crn){
        return post(postSearchUrl)
            .bodyValue("{\"crn\": \"" + crn+ "\"}")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(SearchResponses.class)
            .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(minBackOffSeconds))
                .jitter(0.0d)
                .doAfterRetryAsync(this::logRetrySignal)
                .filter(EXCEPTION_RETRY_FILTER))
            .onErrorResume(throwable -> {
                if (Exceptions.isRetryExhausted(throwable)) {
                    log.error("Retry error on offender search for {}. {} with maximum of {}", crn, throwable.getMessage(), maxRetries);
                    return Mono.error(throwable);
                }
                return Mono.error(throwable);
            })
            ;
    }

    private WebClient.RequestBodySpec post(String uri) {
        WebClient.RequestBodySpec postSpec = webClient
                .post()
                .uri(uri);

        if (!disableAuthentication)  {
            return postSpec.attributes(clientRegistrationId("offender-search-client"));
        } else {
            return postSpec;
        }
    }

    private Mono<? extends MatchResponse> handleError(Throwable throwable) {

        if (Exceptions.isRetryExhausted(throwable)) {
            log.error("Retry error :{} with maximum of {}", throwable.getMessage(), maxRetries);
            return Mono.error(throwable);
        }
        return Mono.error(throwable);
    }

    private Mono<Void> logRetrySignal(RetrySignal retrySignal) {
        log.warn("Error from call to offender search, at attempt {} of {}. Root Cause {} ",
            retrySignal.totalRetries(), maxRetries, retrySignal.failure().getMessage());
        return Mono.empty();
    }

    /**
     * Filter which decides whether or not to retry. Return true if we do wish to retry.
     */
    static final Predicate<? super Throwable> EXCEPTION_RETRY_FILTER = throwable -> {
        boolean retry = true;
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) throwable;
            HttpStatus status = ex.getStatusCode();
            switch (status) {
                case NOT_FOUND:
                case FORBIDDEN:
                case UNAUTHORIZED:
                case TOO_MANY_REQUESTS:
                    retry = false;
                    break;
                default:
                    retry = true;
            }
        }

        return retry;
    };
}
