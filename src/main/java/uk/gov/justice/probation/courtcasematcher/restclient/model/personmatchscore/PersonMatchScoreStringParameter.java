package uk.gov.justice.probation.courtcasematcher.restclient.model.personmatchscore;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class PersonMatchScoreStringParameter {
  @JsonProperty("0")
  private String platformValue;
  @JsonProperty("1")
  private String deliusValue;

  public static PersonMatchScoreStringParameter of(String pacValue, String deliusValue) {
    return PersonMatchScoreStringParameter.builder().platformValue(pacValue).deliusValue(deliusValue).build();
  }
}