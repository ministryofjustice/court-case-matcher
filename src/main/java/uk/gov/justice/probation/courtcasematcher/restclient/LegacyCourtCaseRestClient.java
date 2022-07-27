package uk.gov.justice.probation.courtcasematcher.restclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.restclient.exception.HearingNotFoundException;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.CCSHearing;

import java.nio.charset.StandardCharsets;

@Component("legacy-client")
@Slf4j
public class LegacyCourtCaseRestClient {
    private final CourtCaseServiceRestHelper courtCaseServiceRestHelper;

    @Value("${court-case-service.case-put-url-template}")
    private String courtCasePutTemplate;

    @Autowired
    public LegacyCourtCaseRestClient(CourtCaseServiceRestHelper courtCaseServiceRestHelper) {
        super();
        this.courtCaseServiceRestHelper = courtCaseServiceRestHelper;
    }

    public LegacyCourtCaseRestClient(CourtCaseServiceRestHelper courtCaseServiceRestHelper,
                                     String courtCasePutTemplate
    ) {
        super();
        this.courtCaseServiceRestHelper = courtCaseServiceRestHelper;
        this.courtCasePutTemplate = courtCasePutTemplate;
    }

    public Mono<Hearing> getHearing(final String courtCode, final String caseNo) throws WebClientResponseException {
        final String path = String.format(courtCasePutTemplate, courtCode, caseNo);

        // Get the existing case. Not a problem if it's not there. So return a Mono.empty() if it's not
        return courtCaseServiceRestHelper.get(path)
            .retrieve()
            .onStatus(HttpStatus::isError, (clientResponse) -> handleGetError(clientResponse, courtCode, caseNo))
            .bodyToMono(CCSHearing.class)
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
        final HttpStatus httpStatus = clientResponse.statusCode();
        // This is expected for new cases
        if (HttpStatus.NOT_FOUND.equals(httpStatus)) {
            log.info("Failed to get case for case number {} and court code {}", caseNo, courtCode);
            return Mono.error(new HearingNotFoundException(courtCode, caseNo));
        }
        else if(HttpStatus.UNAUTHORIZED.equals(httpStatus) || HttpStatus.FORBIDDEN.equals(httpStatus)) {
            log.error("HTTP status {} to to GET the case from court case service", httpStatus);
        }
        final var exception = WebClientResponseException.create(httpStatus.value(),
                httpStatus.name(),
                clientResponse.headers().asHttpHeaders(),
                clientResponse.toString().getBytes(),
                StandardCharsets.UTF_8);
        log.error("Unexpected response code {} getting case number {} and court code {}", httpStatus, caseNo, courtCode, exception);
        throw exception;
    }
}
