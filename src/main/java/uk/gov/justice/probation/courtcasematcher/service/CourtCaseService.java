package uk.gov.justice.probation.courtcasematcher.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;
import uk.gov.justice.probation.courtcasematcher.model.mapper.MatchDetails;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchType;

import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class CourtCaseService {

    @Autowired
    private final CourtCaseRestClient restClient;

    @Autowired
    private final OffenderSearchRestClient offenderSearchRestClient;

    public Mono<CourtCase> getCourtCase(CourtCase aCase) {
        return restClient.getCourtCase(aCase.getCourtCode(), aCase.getCaseNo())
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
                            .matchType(MatchType.of(result))
                            .matches(response.getMatches())
                            .exactMatch(response.isExactMatch())
                            .build());
                })
                .orElse(courtCase);
        saveCourtCase(updatedCase);
    }

    public void saveCourtCase(CourtCase courtCase) {
        try {
            restClient.putCourtCase(courtCase.getCourtCode(), courtCase.getCaseNo(), courtCase).block();
        } finally {
            restClient.postMatches(courtCase.getCourtCode(), courtCase.getCaseNo(), courtCase.getGroupedOffenderMatches()).block();
        }
    }

    public Mono<CourtCase> updateProbationStatusDetail(CourtCase courtCase) {
        return offenderSearchRestClient.search(courtCase.getCrn())
            .filter(searchResponses -> searchResponses.getSearchResponses().size() == 1)
            .map(searchResponses -> searchResponses.getSearchResponses().get(0).getProbationStatusDetail())
            .map(probationStatusDetail -> CaseMapper.merge(probationStatusDetail, courtCase))
            .switchIfEmpty(Mono.just(courtCase));
    }

}
