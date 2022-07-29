package uk.gov.justice.probation.courtcasematcher.repository;

import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;

import java.util.List;

public interface CourtCaseRepository {
    Mono<Hearing> getHearing(String hearingId);

    Mono<Hearing> getHearing(String courtCode, String caseNo) throws WebClientResponseException;

    Mono<Void> putHearing(Hearing hearing);

    Mono<Void> postOffenderMatches(String caseId, List<Defendant> defendants);
}
