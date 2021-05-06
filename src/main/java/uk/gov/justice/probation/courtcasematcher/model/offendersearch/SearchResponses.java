package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

import java.util.Collections;
import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
