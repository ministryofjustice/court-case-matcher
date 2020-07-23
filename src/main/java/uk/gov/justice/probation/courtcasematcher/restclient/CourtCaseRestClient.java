package uk.gov.justice.probation.courtcasematcher.restclient;

import com.google.common.eventbus.EventBus;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseSuccessEvent;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.restclient.exception.CourtCaseNotFoundException;
import uk.gov.justice.probation.courtcasematcher.restclient.exception.CourtNotFoundException;

@Component
@Slf4j
public class CourtCaseRestClient {

    private static final String ERR_MSG_FORMAT_PUT_ABSENT = "Unexpected exception when applying PUT to purge absent cases for court '%s'";
    private static final String ERR_MSG_FORMAT_PUT_CASE = "Unexpected exception when applying PUT to update case number '%s' for court '%s'";
    private static final String ERR_MSG_FORMAT_POST_MATCH = "Unexpected exception when POST matches for case number '%s' for court '%s'";

    @Value("${court-case-service.case-put-url-template}")
    private String courtCasePutTemplate;
    @Value("${court-case-service.matches-post-url-template}")
    private String matchesPostTemplate;
    @Value("${court-case-service.purge-absent-put-url-template}")
    private String purgeAbsentPutTemplate;

    private final EventBus eventBus;

    private final WebClient webClient;

    @Autowired
    public CourtCaseRestClient(@Qualifier("court-case-service") WebClient webClient, EventBus eventBus) {
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
            .bodyToMono(CourtCase.class)
            .map(courtCaseResponse -> {
                log.debug("GET succeeded for retrieving the case for path {}", path);
                return courtCaseResponse;
            })
            .onErrorResume((e) -> {
                log.debug("GET failed for retrieving the case for path {}", path);
                return Mono.empty();
            });
    }

    public Disposable putCourtCase(String courtCode, String caseNo, CourtCase courtCase) {
        final String path = String.format(courtCasePutTemplate, courtCode, caseNo);
        final GroupedOffenderMatches offenderMatches = courtCase.getGroupedOffenderMatches();

        return put(path, courtCase)
            .retrieve()
            .onStatus(HttpStatus::isError, (clientResponse) -> handleError(clientResponse, courtCode, caseNo))
            .bodyToMono(CourtCase.class)
            .doOnError(e -> postErrorToBus(String.format(ERR_MSG_FORMAT_PUT_CASE, caseNo, courtCode) + ".Exception : " + e))
            .subscribe(courtCaseApi -> {
                eventBus.post(CourtCaseSuccessEvent.builder().courtCaseApi(courtCaseApi).build());
                postMatches(courtCaseApi.getCourtCode(), courtCaseApi.getCaseNo(), offenderMatches);
            });
    }

    public void postMatches(String courtCode, String caseNo, GroupedOffenderMatches offenderMatches) {

        if (offenderMatches != null) {
            final String path = String.format(matchesPostTemplate, courtCode, caseNo);
            post(path, offenderMatches)
                .retrieve()
                .onStatus(HttpStatus::isError, (clientResponse) -> handleError(clientResponse, courtCode, caseNo))
                .toBodilessEntity()
                .doOnError(e -> log.error(String.format(ERR_MSG_FORMAT_POST_MATCH, caseNo, courtCode) + ".Exception : " + e))
                .subscribe(responseEntity -> {
                    log.info("Successful POST of offender matches. Response location: {} ",
                        Optional.ofNullable(responseEntity.getHeaders().getFirst(HttpHeaders.LOCATION))
                            .orElse("[NOT FOUND]"));
                });
        }
    }

    public void purgeAbsent(String courtCode, Map<LocalDate, List<String>> cases) {

        final String path = String.format(purgeAbsentPutTemplate, courtCode);
        put(path, cases)
            .retrieve()
            .onStatus(HttpStatus::isError, (clientResponse) -> handleError(clientResponse, () -> new CourtNotFoundException(courtCode)))
            .toBodilessEntity()
            .doOnError(e -> log.error(String.format(ERR_MSG_FORMAT_PUT_ABSENT, courtCode) + ".Exception : " + e))
            .subscribe(responseEntity -> {
                log.info("Successful PUT of all cases for purge in court case service for court {}", courtCode);
            });
    }

    private WebClient.RequestHeadersSpec<?> get(String path) {
        return webClient
            .get()
            .uri(uriBuilder -> uriBuilder.path(path).build())
            .accept(MediaType.APPLICATION_JSON);
    }

    private WebClient.RequestHeadersSpec<?> put(String path, CourtCase courtCase) {
        return webClient
            .put()
            .uri(uriBuilder -> uriBuilder.path(path).build())
            .body(Mono.just(courtCase), CourtCase.class)
            .accept(MediaType.APPLICATION_JSON);
    }

    private WebClient.RequestHeadersSpec<?> put(String path, Map<LocalDate, List<String>> casesByDate) {
        return webClient
            .put()
            .uri(uriBuilder -> uriBuilder.path(path).build())
            .body(Mono.just(casesByDate), Map.class)
            .accept(MediaType.APPLICATION_JSON);
    }

    private WebClient.RequestHeadersSpec<?> post(String path, GroupedOffenderMatches request) {
        return webClient
            .post()
            .uri(uriBuilder -> uriBuilder.path(path).build())
            .body(Mono.just(request), GroupedOffenderMatches.class)
            .accept(MediaType.APPLICATION_JSON);
    }

    private Mono<? extends Throwable> handleGetError(ClientResponse clientResponse, String courtCode, String caseNo) {
        final HttpStatus httpStatus = clientResponse.statusCode();
        // This is expected for new cases
        if (HttpStatus.NOT_FOUND.equals(clientResponse.statusCode())) {
            log.info("Failed to get case for case number {} and court code {}", caseNo, courtCode);
            return Mono.error(new CourtCaseNotFoundException(courtCode, caseNo));
        }
        throw WebClientResponseException.create(httpStatus.value(),
            httpStatus.name(),
            clientResponse.headers().asHttpHeaders(),
            clientResponse.toString().getBytes(),
            StandardCharsets.UTF_8);
    }

    private Mono<? extends Throwable> handleError(ClientResponse clientResponse, String courtCode, String caseNo) {
        final HttpStatus httpStatus = clientResponse.statusCode();
        if (HttpStatus.NOT_FOUND.equals(httpStatus)) {
            return Mono.error(new CourtCaseNotFoundException(courtCode, caseNo));
        }
        throw WebClientResponseException.create(httpStatus.value(),
            httpStatus.name(),
            clientResponse.headers().asHttpHeaders(),
            clientResponse.toString().getBytes(),
            StandardCharsets.UTF_8);
    }

    private Mono<? extends Throwable> handleError(ClientResponse clientResponse, Supplier<Throwable> notFoundError) {
        final HttpStatus httpStatus = clientResponse.statusCode();
        if (HttpStatus.NOT_FOUND.equals(httpStatus)) {
            return Mono.error(notFoundError.get());
        }
        throw WebClientResponseException.create(httpStatus.value(),
            httpStatus.name(),
            clientResponse.headers().asHttpHeaders(),
            clientResponse.toString().getBytes(),
            StandardCharsets.UTF_8);
    }

    private void postErrorToBus(String failureMessage) {
        eventBus.post(CourtCaseFailureEvent.builder().failureMessage(failureMessage).build());
    }
}
