package uk.gov.justice.probation.courtcasematcher.restclient;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.application.FeatureFlags;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.repository.CourtCaseRepository;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.CCSExtendedCase;

import java.util.Optional;

@Component("court-case-rest-client")
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class CourtCaseRestClient implements CourtCaseRepository {

    @Autowired
    private FeatureFlags featureFlags;

    @Autowired
    private LegacyCourtCaseRestClient legacyCourtCaseRestClient;

    @Autowired
    private CourtCaseServiceRestHelper restHelper;

    @Value("${court-case-service.case-put-url-template-extended}")
    private String courtCasePutTemplate;

    @Override
    public Mono<CourtCase> getCourtCase(String courtCode, String caseNo) throws WebClientResponseException {
        return legacyCourtCaseRestClient.getCourtCase(courtCode, caseNo);
    }

    @Override
    public Mono<Void> putCourtCase(CourtCase courtCase) {
        final var useLegacy = Optional.ofNullable(featureFlags.getFlags())
                .map(map -> map.get("use-legacy-court-case-rest-client"))
                .orElse(false);

        if (useLegacy) {
            return legacyCourtCaseRestClient.putCourtCase(courtCase);
        }

        final var caseId = courtCase.getCaseId();
        final String path = String.format(courtCasePutTemplate, caseId);
        return restHelper.putObject(path, CCSExtendedCase.of(courtCase), CCSExtendedCase.class)
                .retrieve()
                .toBodilessEntity()
                .retryWhen(restHelper.buildRetrySpec(
                        String.format("Initial retry failed for caseId %s", caseId),
                        (attemptNo, maxAttempts) -> String.format("Retry failed for caseId %s at attempt %s of %s", caseId, attemptNo, maxAttempts))
                )
                .then();
    }

    @Override
    public Mono<Void> postMatches(String courtCode, String caseNo, GroupedOffenderMatches offenderMatches) {
        return legacyCourtCaseRestClient.postMatches(courtCode, caseNo, offenderMatches);
    }
}
