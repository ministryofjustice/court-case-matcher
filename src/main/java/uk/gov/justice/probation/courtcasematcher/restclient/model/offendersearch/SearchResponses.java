package uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonDeserialize(converter = SearchResponsesConverter.class)
public class SearchResponses {

    private final List<SearchResponse> searchResponses;

    public List<SearchResponse> getSearchResponses() {
        return searchResponses != null ? searchResponses : Collections.emptyList();
    }
}
