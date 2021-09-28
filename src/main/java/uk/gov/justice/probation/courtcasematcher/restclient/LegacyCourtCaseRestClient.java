package uk.gov.justice.probation.courtcasematcher.restclient;

import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.tuple.Tuple2;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseSuccessEvent;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.repository.CourtCaseRepository;
import uk.gov.justice.probation.courtcasematcher.restclient.exception.CourtCaseNotFoundException;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.CCSCourtCase;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.CCSGroupedOffenderMatchesRequest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Component("legacy-client")
@Slf4j
public class LegacyCourtCaseRestClient implements CourtCaseRepository {

    private static final String ERR_MSG_FORMAT_PUT_CASE = "Unexpected exception when applying PUT to update case number '%s' for court '%s'.";
    private static final String ERR_MSG_FORMAT_POST_MATCH = "Unexpected exception when POST matches for case number '%s' for court '%s'. Match count was %s";

    private static final String ERROR_MSG_FORMAT_INITIAL_PUT_CASE = "Initial error from PUT of the case %s for court %s. Will retry.";
    private static final String ERROR_MSG_FORMAT_RETRY_PUT_CASE = "Retry error from PUT of the case %s for court %s, at attempt %s of %s.";
    private static final String ERROR_MSG_FORMAT_INITIAL_POST_MATCHES = "Initial error from POST of the offender matches for case %s in court %s, Will retry.";
    private static final String ERROR_MSG_FORMAT_RETRY_POST_MATCHES = "Retry error from POST of the offender matches for case %s in court %s, at attempt %s of %s.";
    private final CourtCaseServiceRestHelper courtCaseServiceRestHelper;

    @Value("${court-case-service.case-put-url-template}")
    private String courtCasePutTemplate;
    @Value("${court-case-service.matches-post-url-template}")
    private String matchesPostTemplate;

    private final EventBus eventBus;

    @Autowired
    public LegacyCourtCaseRestClient(CourtCaseServiceRestHelper courtCaseServiceRestHelper,
                                     EventBus eventBus) {
        super();
        this.courtCaseServiceRestHelper = courtCaseServiceRestHelper;
        this.eventBus = eventBus;
    }

    public LegacyCourtCaseRestClient(CourtCaseServiceRestHelper courtCaseServiceRestHelper,
                                     EventBus eventBus,
                                     String matchesPostTemplate,
                                     String courtCasePutTemplate
    ) {
        super();
        this.courtCaseServiceRestHelper = courtCaseServiceRestHelper;
        this.eventBus = eventBus;
        this.matchesPostTemplate = matchesPostTemplate;
        this.courtCasePutTemplate = courtCasePutTemplate;
    }

    @Override
    public Mono<CourtCase> getCourtCase(final String courtCode, final String caseNo) throws WebClientResponseException {
        final String path = String.format(courtCasePutTemplate, courtCode, caseNo);

        // Get the existing case. Not a problem if it's not there. So return a Mono.empty() if it's not
        return courtCaseServiceRestHelper.get(path)
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

    @Override
    public Mono<Void> putCourtCase(CourtCase courtCase) {
        final var courtCode = courtCase.getCourtCode();
        final var caseNo = courtCase.getCaseNo();
        final String path = String.format(courtCasePutTemplate, courtCode, caseNo);

        return put(path, CCSCourtCase.of(courtCase))
                .retrieve()
                .bodyToMono(CCSCourtCase.class)
                .retryWhen(courtCaseServiceRestHelper.buildRetrySpec(courtCode, caseNo, ERROR_MSG_FORMAT_RETRY_PUT_CASE, ERROR_MSG_FORMAT_INITIAL_PUT_CASE))
                .map(CCSCourtCase::asDomain)
                .doOnSuccess(courtCaseApi -> eventBus.post(CourtCaseSuccessEvent.builder().courtCase(courtCaseApi).build()))
                .doOnError(throwable -> handlePutError(throwable, caseNo, courtCode))
                .doOnError(throwable -> eventBus.post(CourtCaseFailureEvent.builder()
                        .failureMessage(String.format(ERR_MSG_FORMAT_PUT_CASE, caseNo, courtCode))
                        .throwable(throwable)
                        .build()))
                .then();
    }

    @Override
    public Mono<Void> postMatches(String courtCode, String caseNo, GroupedOffenderMatches offenderMatches) {

        return Mono.justOrEmpty(offenderMatches)
            .map(matches -> Tuple2.of(String.format(matchesPostTemplate, courtCode, caseNo), CCSGroupedOffenderMatchesRequest.of(matches)))
            .flatMap(tuple2 -> post(tuple2.getT1(), tuple2.getT2())
                    .retrieve()
                    .toBodilessEntity()
                    .retryWhen(courtCaseServiceRestHelper.buildRetrySpec(courtCode, caseNo, ERROR_MSG_FORMAT_RETRY_POST_MATCHES, ERROR_MSG_FORMAT_INITIAL_POST_MATCHES)))
            .doOnNext(responseEntity -> log.info("Successful POST of offender matches. Response location: {} ",
                    Optional.ofNullable(responseEntity)
                            .map(HttpEntity::getHeaders)
                            .map((HttpHeaders headers) -> headers.getFirst(HttpHeaders.LOCATION))
                            .orElse("[NOT FOUND]")))
            .doOnError(throwable -> log.error(String.format(ERR_MSG_FORMAT_POST_MATCH, courtCode, caseNo, Optional.ofNullable(offenderMatches).map(GroupedOffenderMatches::getMatches).map(List::size)), throwable))
            .then();
    }

    @Override
    public Mono<Void> postDefendantMatches(String caseId, List<Defendant> defendants) {
        return null;
    }

    private WebClient.RequestHeadersSpec<?> put(String path, CCSCourtCase CCSCourtCase) {
        return courtCaseServiceRestHelper.putObject(path, CCSCourtCase, CCSCourtCase.class);
    }

    private WebClient.RequestHeadersSpec<?> post(String path, CCSGroupedOffenderMatchesRequest request) {
        return courtCaseServiceRestHelper.postObject(path, request, CCSGroupedOffenderMatchesRequest.class);
    }

    private void handlePutError(Throwable throwable, String courtCode, String caseNo) {

        if (Exceptions.isRetryExhausted(throwable)) {
            final var maxRetries = courtCaseServiceRestHelper.getMaxRetries();
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

}
