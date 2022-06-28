package uk.gov.justice.probation.courtcasematcher.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;
import uk.gov.justice.probation.courtcasematcher.repository.CourtCaseRepository;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor(onConstructor_ = @Autowired)
@NoArgsConstructor
public class CourtCaseService {


    @Qualifier("court-case-rest-client")
    private CourtCaseRepository courtCaseRepository;

    private OffenderSearchRestClient offenderSearchRestClient;

    public Mono<CourtCase> findCourtCase(CourtCase aCase) {
        if (aCase.getSource() == DataSource.COMMON_PLATFORM) {
            return courtCaseRepository.getCourtCase(aCase.getHearingId());
        }
        return courtCaseRepository.getCourtCase(aCase.getCourtCode(), aCase.getCaseNo());
    }

    public void saveCourtCase(CourtCase courtCase) {
        CourtCase updatedCase = courtCase;

        // If this is a new case from COMMON platform, set caseNo = caseId
        if (courtCase.getSource() == DataSource.COMMON_PLATFORM && courtCase.getCaseNo() == null) {
            updatedCase = courtCase.withCaseNo(updatedCase.getCaseId());
        }

        courtCaseRepository.putCourtCase(updatedCase)
                .doOnError(throwable -> {
                    log.error("Save court case failed for case id {} with {}", courtCase.getCaseId(), throwable.getMessage());
                    throw new RuntimeException(throwable.getMessage());
                })
                .block();

        courtCaseRepository.postOffenderMatches(updatedCase.getCaseId(), updatedCase.getDefendants())
                .block();

    }
    public Mono<CourtCase> updateProbationStatusDetail(CourtCase courtCase) {
        final var updatedDefendants = courtCase.getDefendants()
                .stream()
                .map(defendant -> defendant.getCrn() != null ? updateDefendant(defendant) : Mono.just(defendant))
                .map(Mono::block)
                .collect(Collectors.toList());

        return Mono.just(courtCase.withDefendants(updatedDefendants));
    }

    private Mono<Defendant> updateDefendant(Defendant defendant) {
        return offenderSearchRestClient.search(defendant.getCrn())
                .filter(searchResponses -> searchResponses.getSearchResponses().size() == 1)
                .map(searchResponses -> searchResponses.getSearchResponses().get(0).getProbationStatusDetail())
                .map(probationStatusDetail -> CaseMapper.merge(probationStatusDetail, defendant))
                .defaultIfEmpty(defendant);
    }
}
