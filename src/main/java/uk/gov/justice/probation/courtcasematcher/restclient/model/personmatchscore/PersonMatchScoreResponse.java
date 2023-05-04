package uk.gov.justice.probation.courtcasematcher.restclient.model.personmatchscore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonMatchScoreResponse {
  @JsonProperty("match_probability")
  private PersonMatchScoreParameter<Double> matchProbability;
}
