package uk.gov.justice.probation.courtcasematcher.service;

import lombok.Builder;
import lombok.Data;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchRequest;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchResponse;

@Data
@Builder
public class SearchResult {
    private MatchResponse matchResponse;
    private MatchRequest matchRequest;
}
