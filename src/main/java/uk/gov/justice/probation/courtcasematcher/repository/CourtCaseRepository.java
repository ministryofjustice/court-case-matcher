package uk.gov.justice.probation.courtcasematcher.repository;

import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;

import java.util.List;

public interface CourtCaseRepository {
    Mono<CourtCase> getCourtCase(String courtCode, String caseNo) throws WebClientResponseException;

    Mono<Void> putCourtCase(CourtCase courtCase);

    Mono<Void> postDefendantMatches(String caseId, List<Defendant> defendants);
}
