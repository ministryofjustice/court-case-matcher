package uk.gov.justice.probation.courtcasematcher.restclient;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.tuple.Tuple2;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.restclient.exception.HearingNotFoundException;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.CCSExtendedHearing;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.CCSGroupedOffenderMatchesRequest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Component("court-case-rest-client")
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class CourtCaseServiceClient {

    private static final String ERR_MSG_FORMAT_POST_MATCHES = "Unexpected exception when POST matches for case id '%s'";
    private static final String ERROR_MSG_FORMAT_INITIAL_POST_MATCHES = "Initial error from POST of the offender matches for case id %s for defendant id %s, Will retry.";
    private static final String ERROR_MSG_FORMAT_RETRY_POST_MATCHES = "Retry error from POST of the offender matches for case id %s for defendant id %s, at attempt %s of %s.";

    @Autowired
    private LegacyCourtCaseRestClient legacyCourtCaseRestClient;

    @Autowired
    private CourtCaseServiceRestHelper restHelper;

    @Value("${court-case-service.case-by-hearing-id-url-template}")
    private String courtCaseByHearingIdTemplate;

    @Value("${court-case-service.case-by-hearing-id-court-case-id-url-template}")
    private String courtCaseByHearingIdAndCourtCaseIdTemplate;

    @Value("${court-case-service.matches-by-case-defendant-post-url-template}")
    private String matchesPostTemplate;

    public Mono<Hearing> getHearing(String hearingId) {
        final String path = String.format(courtCaseByHearingIdTemplate, hearingId);

        // Get the existing case. Not a problem if it's not there. So return a Mono.empty() if it's not
        return restHelper.get(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (clientResponse) -> handleGetError(clientResponse, hearingId))
                .bodyToMono(CCSExtendedHearing.class)
                .map(response -> {
                    log.debug("GET succeeded for the hearing at {}", path);
                    return response.asDomain();
                })
                .onErrorResume(HearingNotFoundException.class, (e) -> {
                    // This is normal in the context of CCM, don't log
                    return Mono.empty();
                });
    }

    public Mono<Hearing> getHearing(String hearingId, String courtCaseId) {
        final String path = String.format(courtCaseByHearingIdAndCourtCaseIdTemplate, hearingId, courtCaseId);

        // Get the existing case. Not a problem if it's not there. So return a Mono.empty() if it's not
        return restHelper.get(path)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (clientResponse) -> handleGetError(clientResponse, hearingId))
            .bodyToMono(CCSExtendedHearing.class)
            .map(response -> {
                log.debug("GET succeeded for the hearing at {}", path);
                return response.asDomain();
            })
            .onErrorResume(HearingNotFoundException.class, (e) -> {
                // This is normal in the context of CCM, don't log
                return Mono.empty();
            });
    }

    public Mono<Hearing> getHearing(String courtCode, String caseNo, String listNo) throws WebClientResponseException {
        return legacyCourtCaseRestClient.getHearing(courtCode, caseNo, listNo);
    }


    public Mono<Void> putHearing(Hearing hearing) {
        final var ccsExtendedHearing = CCSExtendedHearing.of(hearing);
        final var hearingId = ccsExtendedHearing.getHearingId();
        final var path = String.format(courtCaseByHearingIdTemplate, hearingId);
        return restHelper.putObject(path, ccsExtendedHearing, CCSExtendedHearing.class)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> log.debug("PUT succeeded for the hearing at {}", path))
                .retryWhen(restHelper.buildRetrySpec(
                        String.format("Initial retry failed for hearingId %s", hearingId),
                        (attemptNo, maxAttempts) -> String.format("Retry failed for hearingId %s at attempt %s of %s", hearingId, attemptNo, maxAttempts))
                )
                .then();
    }

    private Mono<Void> postOffenderMatches(String caseId, String defendantId, GroupedOffenderMatches offenderMatches) {
        return Mono.justOrEmpty(offenderMatches)
            .map(matches -> Tuple2.of(String.format(matchesPostTemplate, defendantId), CCSGroupedOffenderMatchesRequest.of(matches)))
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

    public Mono<Void> postOffenderMatches(String caseId, List<Defendant> defendants) {
        return Flux.fromStream(defendants.stream())
                .flatMap(defendant -> postOffenderMatches(caseId, defendant.getDefendantId(), defendant.getGroupedOffenderMatches()))
                .then();
    }

    private Mono<? extends Throwable> handleGetError(ClientResponse clientResponse, String hearingId) {
        final HttpStatusCode httpStatusCode = clientResponse.statusCode();
        // This is expected for new hearings
        if (HttpStatus.NOT_FOUND.equals(httpStatusCode)) {
            log.info("Failed to get hearing for hearingId {}", hearingId);
            return Mono.error(new HearingNotFoundException(hearingId));
        }
        else if(HttpStatus.UNAUTHORIZED.equals(httpStatusCode) || HttpStatus.FORBIDDEN.equals(httpStatusCode)) {
            log.error("HTTP status {} to to GET the hearing from court case service", httpStatusCode);
        }
        throw WebClientResponseException.create(httpStatusCode.value(),
                httpStatusCode.toString(),
                clientResponse.headers().asHttpHeaders(),
                clientResponse.toString().getBytes(),
                StandardCharsets.UTF_8);
    }
}
