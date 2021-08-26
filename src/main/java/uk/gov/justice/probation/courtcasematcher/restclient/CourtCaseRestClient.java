package uk.gov.justice.probation.courtcasematcher.restclient;

import com.google.common.eventbus.EventBus;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.tuple.Tuple2;
import reactor.util.retry.Retry;
import reactor.util.retry.Retry.RetrySignal;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseSuccessEvent;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.restclient.exception.CourtCaseNotFoundException;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.CCSCourtCase;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.GroupedOffenderMatchesRequest;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;
import static uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient.EXCEPTION_RETRY_FILTER;

@Component
@Slf4j
public class CourtCaseRestClient {

    private static final String ERR_MSG_FORMAT_PUT_CASE = "Unexpected exception when applying PUT to update case number '%s' for court '%s'.";
    private static final String ERR_MSG_FORMAT_POST_MATCH = "Unexpected exception when POST matches for case number '%s' for court '%s'. Match count was %s";

    private static final String ERROR_MSG_FORMAT_INITIAL_CASE = "Initial error from PUT of the case %s for court %s. Will retry.";
    private static final String ERROR_MSG_FORMAT_RETRY_PUT_CASE = "Retry error from PUT of the case %s for court %s, at attempt %s of %s.";
    private static final String ERROR_MSG_FORMAT_INITIAL_MATCHES = "Initial error from POST of the offender matches for case %s in court %s, Will retry.";
    private static final String ERROR_MSG_FORMAT_RETRY_POST_MATCHES = "Retry error from POST of the offender matches for case %s in court %s, at attempt %s of %s.";

    @Value("${court-case-service.case-put-url-template}")
    private String courtCasePutTemplate;
    @Value("${court-case-service.matches-post-url-template}")
    private String matchesPostTemplate;

    @Value("${court-case-service.disable-authentication:false}")
    private Boolean disableAuthentication;

    private final EventBus eventBus;

    private final WebClient webClient;

    @Setter
    @Value("${court-case-service.max-retries:3}")
    private int maxRetries;

    @Setter
    @Value("${court-case-service.min-backoff-seconds:3}")
    private int minBackOffSeconds;

    @Autowired
    public CourtCaseRestClient(@Qualifier("courtCaseServiceWebClient") WebClient webClient, EventBus eventBus) {
        super();
        this.webClient = webClient;
        this.eventBus = eventBus;
    }

    public Mono<CourtCase> getCourtCase(final String courtCode, final String caseNo) throws WebClientResponseException {
        final String path = String.format(courtCasePutTemplate, courtCode, caseNo);

        // Get the existing case. Not a problem if it's not there. So return a Mono.empty() if it's not
        return get(path)
            .retrieve()
            .onStatus(HttpStatus::isError, (clientResponse) -> handleGetError(clientResponse, courtCode, caseNo))
            .bodyToMono(CCSCourtCase.class)
            .map(courtCaseResponse -> {
                log.debug("GET succeeded for retrieving the case for path {}", path);
                return courtCaseResponse.asDomain();
            })
            .onErrorResume((e) -> {
                // This is normal in the context of CCM, don't log
                return Mono.empty();
            });
    }

    public Mono<Void> putCourtCase(String courtCode, String caseNo, CourtCase courtCase) {
        final String path = String.format(courtCasePutTemplate, courtCode, caseNo);

        return put(path, CCSCourtCase.of(courtCase))
                .retrieve()
                .bodyToMono(CourtCase.class)
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(minBackOffSeconds))
                        .jitter(0.0d)
                        .doAfterRetryAsync((retrySignal) -> logRetrySignal(retrySignal, ERROR_MSG_FORMAT_RETRY_PUT_CASE, ERROR_MSG_FORMAT_INITIAL_CASE, courtCode, caseNo))
                        .filter(EXCEPTION_RETRY_FILTER))
                .doOnSuccess(courtCaseApi -> eventBus.post(CourtCaseSuccessEvent.builder().courtCase(courtCaseApi).build()))
                .doOnError(throwable -> handleError(throwable, caseNo, courtCode))
                .doOnError(throwable -> eventBus.post(CourtCaseFailureEvent.builder()
                        .failureMessage(String.format(ERR_MSG_FORMAT_PUT_CASE, caseNo, courtCode))
                        .throwable(throwable)
                        .build()))
                .then();
    }

    public Mono<Void> postMatches(String courtCode, String caseNo, GroupedOffenderMatches offenderMatches) {

        return Mono.justOrEmpty(offenderMatches)
            .map(matches -> Tuple2.of(String.format(matchesPostTemplate, courtCode, caseNo), GroupedOffenderMatchesRequest.of(matches)))
            .flatMap(tuple2 -> post(tuple2.getT1(), tuple2.getT2())
                    .retrieve()
                    .toBodilessEntity()
                    .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(minBackOffSeconds))
                    .jitter(0.0d)
                    .doAfterRetryAsync((retrySignal) -> logRetrySignal(retrySignal, ERROR_MSG_FORMAT_RETRY_POST_MATCHES, ERROR_MSG_FORMAT_INITIAL_MATCHES, courtCode, caseNo))
                    .filter(EXCEPTION_RETRY_FILTER)))
            .doOnNext(responseEntity -> log.info("Successful POST of offender matches. Response location: {} ",
                    Optional.ofNullable(responseEntity)
                            .map(HttpEntity::getHeaders)
                            .map((HttpHeaders headers) -> headers.getFirst(HttpHeaders.LOCATION))
                            .orElse("[NOT FOUND]")))
            .doOnError(throwable -> log.error(String.format(ERR_MSG_FORMAT_POST_MATCH, courtCode, caseNo, Optional.ofNullable(offenderMatches).map(GroupedOffenderMatches::getMatches).map(List::size)), throwable))
            .then();
    }

    private WebClient.RequestHeadersSpec<?> get(String path) {
        final WebClient.RequestHeadersSpec<?> spec = webClient
            .get()
            .uri(uriBuilder -> uriBuilder.path(path).build())
            .accept(MediaType.APPLICATION_JSON);

        return addSpecAuthAttribute(spec, path);
    }

    private WebClient.RequestHeadersSpec<?> put(String path, CCSCourtCase CCSCourtCase) {
        WebClient.RequestHeadersSpec<?> spec =  webClient
            .put()
            .uri(uriBuilder -> uriBuilder.path(path).build())
            .body(Mono.just(CCSCourtCase), CourtCase.class)
            .accept(MediaType.APPLICATION_JSON);

        return addSpecAuthAttribute(spec, path);
    }

    private WebClient.RequestHeadersSpec<?> post(String path, GroupedOffenderMatchesRequest request) {
        WebClient.RequestHeadersSpec<?> spec = webClient
            .post()
            .uri(uriBuilder -> uriBuilder.path(path).build())
            .body(Mono.just(request), GroupedOffenderMatches.class)
            .accept(MediaType.APPLICATION_JSON);

        return addSpecAuthAttribute(spec, path);
    }

    private RequestHeadersSpec<?> addSpecAuthAttribute(RequestHeadersSpec<?> spec, String path) {
        if (disableAuthentication) {
            return spec;
        }

        log.info(String.format("Authenticating with %s for call to %s", "offender-search-client", path));
        return spec.attributes(clientRegistrationId("offender-search-client"));
    }

    private void handleError(Throwable throwable, String courtCode, String caseNo) {

        if (Exceptions.isRetryExhausted(throwable)) {
            log.error(String.format(ERROR_MSG_FORMAT_RETRY_PUT_CASE, caseNo, courtCode, maxRetries, maxRetries));
        }
    }

    private Mono<? extends Throwable> handleGetError(ClientResponse clientResponse, String courtCode, String caseNo) {
        final HttpStatus httpStatus = clientResponse.statusCode();
        // This is expected for new cases
        if (HttpStatus.NOT_FOUND.equals(httpStatus)) {
            log.info("Failed to get case for case number {} and court code {}", caseNo, courtCode);
            return Mono.error(new CourtCaseNotFoundException(courtCode, caseNo));
        }
        else if(HttpStatus.UNAUTHORIZED.equals(httpStatus) || HttpStatus.FORBIDDEN.equals(httpStatus)) {
            log.error("HTTP status {} to to GET the case from court case service", httpStatus);
        }
        throw WebClientResponseException.create(httpStatus.value(),
            httpStatus.name(),
            clientResponse.headers().asHttpHeaders(),
            clientResponse.toString().getBytes(),
            StandardCharsets.UTF_8);
    }

    private Mono<Void> logRetrySignal(RetrySignal retrySignal, String messageFormat, String initialMessageFormat, String courtCode, String caseNo) {
        if (retrySignal.totalRetries() > 0 ) {
            log.warn(String.format(messageFormat, caseNo, courtCode, retrySignal.totalRetries(), maxRetries));
        }
        else {
            log.warn(String.format(initialMessageFormat, caseNo, courtCode));
        }
        return Mono.empty();
    }
}
