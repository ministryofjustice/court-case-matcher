package uk.gov.justice.probation.courtcasematcher.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;
import uk.gov.justice.probation.courtcasematcher.model.mapper.MatchDetails;
import uk.gov.justice.probation.courtcasematcher.repository.CourtCaseRepository;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OffenderSearchMatchType;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.SearchResult;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class CourtCaseService {

    @Autowired
    @Qualifier("court-case-rest-client")
    private CourtCaseRepository courtCaseRepository;

    @Autowired
    private OffenderSearchRestClient offenderSearchRestClient;

    public Mono<CourtCase> getCourtCase(CourtCase aCase) {
        if (aCase.getCaseNo() == null) {
            log.warn(String.format("caseNo not available for %s case with id '%s'. Skipping check for existing case.", aCase.getSource(), aCase.getCaseId()));
            return Mono.just(aCase);
        }
        return courtCaseRepository.getCourtCase(aCase.getCourtCode(), aCase.getCaseNo())
                .map(existing -> CaseMapper.merge(aCase, existing))
                .switchIfEmpty(Mono.defer(() -> Mono.just(aCase)));
    }

    public void createCase(CourtCase courtCase, SearchResult searchResult) {
        final var updatedCase = Optional.ofNullable(searchResult)
                .map(result -> {
                    var response = result.getMatchResponse();
                    log.debug("Save court case with search response for case {}, court {}",
                            courtCase.getCaseNo(), courtCase.getCourtCode());
                    return CaseMapper.newFromCourtCaseWithMatches(courtCase, MatchDetails.builder()
                            .matchType(OffenderSearchMatchType.domainMatchTypeOf(result))
                            .matches(response.getMatches())
                            .exactMatch(response.isExactMatch())
                            .build());
                })
                .orElse(courtCase);
        saveCourtCase(updatedCase);
    }

    public void saveCourtCase(CourtCase courtCase) {
        // TODO: This is temporary, to be replaced with call to postOffenderMatches using caseId as primary key
        // Unclear which bit is temporary - the IF statement or the postMatches bit ?
        CourtCase updatedCase = courtCase;
        if(courtCase.getCaseNo() == null) {
            final var caseId = UUID.randomUUID().toString();
            updatedCase = courtCase.withCaseId(caseId)
                    .withCaseNo(caseId);
        }
        try {
            courtCaseRepository.putCourtCase(updatedCase).block();
        } finally {
            courtCaseRepository.postOffenderMatches(updatedCase.getCaseId(), updatedCase.getFirstDefendant().getDefendantId(), updatedCase.getGroupedOffenderMatches()).block();
        }
    }

    public Mono<CourtCase> updateProbationStatusDetail(CourtCase courtCase) {
        return offenderSearchRestClient.search(courtCase.getFirstDefendant().getCrn())
                .filter(searchResponses -> searchResponses.getSearchResponses().size() == 1)
                .map(searchResponses -> searchResponses.getSearchResponses().get(0).getProbationStatusDetail())
                .map(probationStatusDetail -> CaseMapper.merge(probationStatusDetail, courtCase))
                .switchIfEmpty(Mono.just(courtCase));
    }

}
