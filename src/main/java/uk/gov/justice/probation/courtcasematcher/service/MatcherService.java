package uk.gov.justice.probation.courtcasematcher.service;

import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;

@Service
@Slf4j
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class MatcherService {

    @Autowired
    private final CourtCaseRestClient restClient;

    @Autowired
    private final OffenderSearchRestClient offenderSearchRestClient;

    public Mono<SearchResponse> getSearchResponse(String defendantName, LocalDate dateOfBirth, String courtCode, String caseNo) {
        return offenderSearchRestClient.search(defendantName, dateOfBirth)
                .map(searchResponse -> {
                    log.info(String.format("Match results for caseNo: %s, courtCode: %s - matchedBy: %s, matchCount: %s",
                        caseNo, courtCode, searchResponse.getMatchedBy(), searchResponse.getMatches() == null ? "null" : searchResponse.getMatches().size()));
                    return searchResponse;
                })
                .flatMap(searchResponse -> {
                    List<Match> matches = searchResponse.getMatches();
                    if (matches != null && matches.size() == 1) {
                        return Mono.zip(Mono.just(searchResponse), restClient.getProbationStatus(matches.get(0).getOffender().getOtherIds().getCrn()));
                    }
                    else {
                        log.debug("Got {} matches for defendant name {}, dob {}, match type {}",
                            searchResponse.getMatches().size(), defendantName, dateOfBirth, searchResponse.getMatchedBy());
                        return Mono.zip(Mono.just(searchResponse), Mono.just(""));
                    }
                })
                .map(tuple2 -> combine(tuple2.getT1(), tuple2.getT2()))
                .doOnSuccess((data) -> {
                    if (data == null) {
                        log.error(String.format("Match results for caseNo: %s, courtCode: %s - Empty response from OffenderSearchRestClient",
                            caseNo, courtCode));
                    }
                });
    }

    public SearchResponse combine(SearchResponse searchResponse, String probationStatus) {
        return SearchResponse.builder().matchedBy(searchResponse.getMatchedBy())
            .probationStatus(StringUtils.isEmpty(probationStatus) ? null : probationStatus)
            .matches(searchResponse.getMatches())
            .build();
    }

}
