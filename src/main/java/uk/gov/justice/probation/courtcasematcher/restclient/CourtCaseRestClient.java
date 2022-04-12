package uk.gov.justice.probation.courtcasematcher.restclient;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.tuple.Tuple2;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.repository.CourtCaseRepository;
import uk.gov.justice.probation.courtcasematcher.restclient.exception.CourtCaseNotFoundException;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.CCSExtendedCase;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.CCSGroupedOffenderMatchesRequest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Component("court-case-rest-client")
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class CourtCaseRestClient implements CourtCaseRepository {

    private static final String ERR_MSG_FORMAT_POST_MATCHES = "Unexpected exception when POST matches for case id '%s'";
    private static final String ERROR_MSG_FORMAT_INITIAL_POST_MATCHES = "Initial error from POST of the offender matches for case id %s for defendant id %s, Will retry.";
    private static final String ERROR_MSG_FORMAT_RETRY_POST_MATCHES = "Retry error from POST of the offender matches for case id %s for defendant id %s, at attempt %s of %s.";

    @Autowired
    private LegacyCourtCaseRestClient legacyCourtCaseRestClient;

    @Autowired
    private CourtCaseServiceRestHelper restHelper;

    @Value("${court-case-service.case-get-by-hearing-id-url-template}")
    private String courtCaseGetByHearingIdTemplate;

    @Value("${court-case-service.case-put-url-template-extended}")
    private String courtCasePutTemplate;

    @Value("${court-case-service.matches-by-case-defendant-post-url-template}")
    private String matchesPostTemplate;

    public Mono<CourtCase> getCourtCase(String hearingId) {
        final String path = String.format(courtCaseGetByHearingIdTemplate, hearingId);

        // Get the existing case. Not a problem if it's not there. So return a Mono.empty() if it's not
        return restHelper.get(path)
                .retrieve()
                .onStatus(HttpStatus::isError, (clientResponse) -> handleGetError(clientResponse, hearingId))

                .bodyToMono(CCSExtendedCase.class)
                .map(courtCaseResponse -> {
                    log.debug("GET succeeded for retrieving the case for path {}", path);
                    return courtCaseResponse.asDomain();
                })
                .onErrorResume(CourtCaseNotFoundException.class, (e) -> {
                    // This is normal in the context of CCM, don't log
                    return Mono.empty();
                });
    }

    @Override
    public Mono<CourtCase> getCourtCase(String courtCode, String caseNo) throws WebClientResponseException {
        return legacyCourtCaseRestClient.getCourtCase(courtCode, caseNo);
    }

    @Override
    public Mono<Void> putCourtCase(CourtCase courtCase) {
        final var extendedCase = CCSExtendedCase.of(courtCase);
        final var caseId = extendedCase.getCaseId();
        final String path = String.format(courtCasePutTemplate, caseId);
        return restHelper.putObject(path, extendedCase, CCSExtendedCase.class)
                .retrieve()
                .toBodilessEntity()
                .retryWhen(restHelper.buildRetrySpec(
                        String.format("Initial retry failed for caseId %s", caseId),
                        (attemptNo, maxAttempts) -> String.format("Retry failed for caseId %s at attempt %s of %s", caseId, attemptNo, maxAttempts))
                )
                .then();
    }

    private Mono<Void> postOffenderMatches(String caseId, String defendantId, GroupedOffenderMatches offenderMatches) {
        return Mono.justOrEmpty(offenderMatches)
            .map(matches -> Tuple2.of(String.format(matchesPostTemplate, caseId, defendantId), CCSGroupedOffenderMatchesRequest.of(matches)))
            .flatMap(tuple2 -> restHelper.postObject(tuple2.getT1(), tuple2.getT2(), CCSGroupedOffenderMatchesRequest.class)
                .retrieve()
                .toBodilessEntity()
                .retryWhen(restHelper.buildRetrySpec(caseId, defendantId, ERROR_MSG_FORMAT_RETRY_POST_MATCHES, ERROR_MSG_FORMAT_INITIAL_POST_MATCHES)))
            .doOnNext(responseEntity -> log.info("Successful POST of offender matches. Response location: {} ",
                Optional.ofNullable(responseEntity)
                    .map(HttpEntity::getHeaders)
                    .map((HttpHeaders headers) -> headers.getFirst(HttpHeaders.LOCATION))
                    .orElse("[NOT FOUND]")))
            .doOnError(throwable -> log.error(String.format(ERR_MSG_FORMAT_POST_MATCHES, caseId), throwable))
            .then();
    }

    @Override
    public Mono<Void> postOffenderMatches(String caseId, List<Defendant> defendants) {
        return Flux.fromStream(defendants.stream())
                .flatMap(defendant -> postOffenderMatches(caseId, defendant.getDefendantId(), defendant.getGroupedOffenderMatches()))
                .then();
    }

    private Mono<? extends Throwable> handleGetError(ClientResponse clientResponse, String caseId) {
        final HttpStatus httpStatus = clientResponse.statusCode();
        // This is expected for new cases
        if (HttpStatus.NOT_FOUND.equals(httpStatus)) {
            log.info("Failed to get case for caseId {}", caseId);
            return Mono.error(new CourtCaseNotFoundException(caseId));
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
