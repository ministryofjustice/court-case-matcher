package uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchResult {
    private MatchResponse matchResponse;
    private MatchRequest matchRequest;
}
