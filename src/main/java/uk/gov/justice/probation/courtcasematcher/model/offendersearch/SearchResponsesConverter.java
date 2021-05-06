package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdConverter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class SearchResponsesConverter extends StdConverter<List<Map<String, Object>>, SearchResponses> {

    private final ObjectMapper objectMapper;

    public SearchResponsesConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public SearchResponses convert(List<Map<String, Object>> value) {
        List<SearchResponse> list = Optional.ofNullable(value).orElse(Collections.emptyList())
            .stream()
            .map(map -> objectMapper.convertValue(map, SearchResponse.class))
            .collect(Collectors.toList());
        return SearchResponses.builder().searchResponses(list).build();
    }

}
