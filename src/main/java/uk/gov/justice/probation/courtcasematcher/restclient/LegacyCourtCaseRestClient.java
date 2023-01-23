package uk.gov.justice.probation.courtcasematcher.restclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.restclient.exception.HearingNotFoundException;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.CCSLibraHearing;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Component("legacy-client")
@Slf4j
public class LegacyCourtCaseRestClient {
    private final CourtCaseServiceRestHelper courtCaseServiceRestHelper;

    @Value("${court-case-service.case-put-url-template}")
    private String caseByCaseNoTemplate;

    @Value("${feature.flags.use-list-no-to-fetch-libra-case}")
    private boolean useListNoToFetchLibraCase;

    @Autowired
    public LegacyCourtCaseRestClient(CourtCaseServiceRestHelper courtCaseServiceRestHelper) {
        super();
        this.courtCaseServiceRestHelper = courtCaseServiceRestHelper;
    }

    public LegacyCourtCaseRestClient(CourtCaseServiceRestHelper courtCaseServiceRestHelper,
                                     String caseByCaseNoTemplate,
                                     boolean useListNoToFetchLibraCase
    ) {
        super();
        this.courtCaseServiceRestHelper = courtCaseServiceRestHelper;
        this.caseByCaseNoTemplate = caseByCaseNoTemplate;
        this.useListNoToFetchLibraCase = useListNoToFetchLibraCase;
    }

    public Mono<Hearing> getHearing(final String courtCode, final String caseNo, final String listNo) throws WebClientResponseException {
        final String path = String.format(caseByCaseNoTemplate, courtCode, caseNo);

        // Get the existing case. Not a problem if it's not there. So return a Mono.empty() if it's not
        WebClient.RequestHeadersSpec<?> getLibraCase = useListNoToFetchLibraCase ?
          courtCaseServiceRestHelper.get(path, Collections.singletonMap("listNo", List.of(listNo)))
          : courtCaseServiceRestHelper.get(path);
        return getLibraCase
            .retrieve()
            .onStatus(HttpStatusCode::isError, (clientResponse) -> handleGetError(clientResponse, courtCode, caseNo))
            .bodyToMono(CCSLibraHearing.class)
            .map(ccsHearing -> {
                log.debug("GET succeeded for retrieving the hearing for path {}", path);
                return ccsHearing.asDomain();
            })
            .onErrorResume((e) -> {
                // This is normal in the context of CCM, don't log
                return Mono.empty();
            });
    }

    private Mono<? extends Throwable> handleGetError(ClientResponse clientResponse, String courtCode, String caseNo) {
        final HttpStatusCode httpStatusCode = clientResponse.statusCode();
        // This is expected for new cases
        if (HttpStatus.NOT_FOUND.equals(httpStatusCode)) {
            log.info("Failed to get case for case number {} and court code {}", caseNo, courtCode);
            return Mono.error(new HearingNotFoundException(courtCode, caseNo));
        }
        else if(HttpStatus.UNAUTHORIZED.equals(httpStatusCode) || HttpStatus.FORBIDDEN.equals(httpStatusCode)) {
            log.error("HTTP status {} to to GET the case from court case service", httpStatusCode);
        }
        final var exception = WebClientResponseException.create(httpStatusCode.value(),
                httpStatusCode.toString(),
                clientResponse.headers().asHttpHeaders(),
                clientResponse.toString().getBytes(),
                StandardCharsets.UTF_8);
        log.error("Unexpected response code {} getting case number {} and court code {}", httpStatusCode, caseNo, courtCode, exception);
        throw exception;
    }
}
