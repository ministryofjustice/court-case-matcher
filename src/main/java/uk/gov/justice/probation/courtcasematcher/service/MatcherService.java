package uk.gov.justice.probation.courtcasematcher.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchRequest;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.SearchResult;

@Service
@Slf4j
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class MatcherService {

    @Autowired
    private final OffenderSearchRestClient offenderSearchRestClient;

    @Autowired
    private final MatchRequest.Factory matchRequestFactory;

    // TODO: Change return type to CourtCase
    public Mono<SearchResult> getSearchResponse(CourtCase courtCase) {
        // TODO: Stream over courtCase.getDefendants()
        final MatchRequest matchRequest;
        try {
            matchRequest = matchRequestFactory.buildFrom(courtCase);
        } catch (Exception e) {
            log.warn(String.format("Unable to create MatchRequest for caseNo: %s, courtCode: %s", courtCase.getCaseNo(), courtCase.getCourtCode()), e);
            throw e;
        }
        return Mono.defer(() -> Mono.just(matchRequest))
                .flatMap(offenderSearchRestClient::match)
                .map(searchResponse -> {
                    log.info(String.format("Match results for caseNo: %s, courtCode: %s - matchedBy: %s, matchCount: %s",
                            courtCase.getCaseNo(), courtCase.getCourtCode(), searchResponse.getMatchedBy(), searchResponse.getMatches() == null ? "null" : searchResponse.getMatches().size()));
                    return searchResponse;
                })
                .doOnSuccess((data) -> {
                    if (data == null) {
                        log.info(String.format("Match results for caseNo: %s, courtCode: %s - Empty response from OffenderSearchRestClient",
                                courtCase.getCaseNo(), courtCase.getCourtCode()));
                    }
                })
                .map(searchResponse -> SearchResult.builder()
                        .matchResponse(searchResponse)
                        .matchRequest(matchRequest)
                        .build());
    }

}
