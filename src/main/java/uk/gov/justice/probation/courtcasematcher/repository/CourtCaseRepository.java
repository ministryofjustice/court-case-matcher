package uk.gov.justice.probation.courtcasematcher.repository;

import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;

public interface CourtCaseRepository {
    Mono<CourtCase> getCourtCase(String courtCode, String caseNo) throws WebClientResponseException;

    Mono<Void> putCourtCase(String courtCode, String caseNo, CourtCase courtCase);

    Mono<Void> postMatches(String courtCode, String caseNo, GroupedOffenderMatches offenderMatches);
}
