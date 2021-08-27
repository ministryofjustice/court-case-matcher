package uk.gov.justice.probation.courtcasematcher.restclient;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.repository.CourtCaseRepository;

//@Component
@AllArgsConstructor
@NoArgsConstructor
public class CourtCaseRestClient implements CourtCaseRepository {

    @Autowired
    private LegacyCourtCaseRestClient legacyCourtCaseRestClient;

    @Override
    public Mono<CourtCase> getCourtCase(String courtCode, String caseNo) throws WebClientResponseException {
        return legacyCourtCaseRestClient.getCourtCase(courtCode, caseNo);
    }

    @Override
    public Mono<Void> putCourtCase(CourtCase courtCase) {
        return null;
    }

    @Override
    public Mono<Void> postMatches(String courtCode, String caseNo, GroupedOffenderMatches offenderMatches) {
        return legacyCourtCaseRestClient.postMatches(courtCode, caseNo, offenderMatches);
    }
}
