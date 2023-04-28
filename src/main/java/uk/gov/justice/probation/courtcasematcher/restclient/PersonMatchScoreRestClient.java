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
import uk.gov.justice.probation.courtcasematcher.restclient.model.personmatchscore.PersonMatchScoreRequest;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personmatchscore.PersonMatchScoreResponse;

import java.time.Duration;
import java.util.function.Predicate;

@Component
@Slf4j
public class PersonMatchScoreRestClient {

    @Setter
    @Value("${person-match-score.post-match-url}")
    private String postMatchUrl;

    @Setter
    @Value("${offender-search.max-retries:3}")
    private int maxRetries;

    @Setter
    @Value("${offender-search.min-backoff-seconds:5}")
    private int minBackOffSeconds;

    private final WebClient webClient;

    @Autowired
    public PersonMatchScoreRestClient(@Qualifier("personMatchScoreWebClient") WebClient webClient) {
        super();
        this.webClient = webClient;
    }
    public Mono<PersonMatchScoreResponse> match(PersonMatchScoreRequest body){

        return webClient.post()
            .uri(postMatchUrl)
                .body(BodyInserters.fromPublisher(Mono.just(body), PersonMatchScoreRequest.class))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(PersonMatchScoreResponse.class)
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(minBackOffSeconds))
                                .jitter(0.0d)
                                .doAfterRetryAsync(this::logRetrySignal)
                                .filter(EXCEPTION_RETRY_FILTER))
                .onErrorResume(this::handleError)
            ;
    }

    private Mono<? extends PersonMatchScoreResponse> handleError(Throwable throwable) {

        if (Exceptions.isRetryExhausted(throwable)) {
            log.error("Retry error :{} with maximum of {}", throwable.getMessage(), maxRetries);
            return Mono.error(throwable);
        }
        return Mono.error(throwable);
    }

    private Mono<Void> logRetrySignal(RetrySignal retrySignal) {
        log.warn("Error from call to person match score, at attempt {} of {}. Root Cause {} ",
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
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
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
