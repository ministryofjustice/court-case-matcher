package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.ProbationStatusDetail;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResponse {

    private final Long offenderId;

    private final OtherIds otherIds;

    @JsonProperty(value = "probationStatus")
    private final ProbationStatusDetail probationStatusDetail;

}
