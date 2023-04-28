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
public class PersonMatchScoreRequest {
  @JsonProperty("unique_id")
  private PersonMatchScoreStringParameter uniqueId;

  @JsonProperty("first_name")
  private PersonMatchScoreStringParameter firstName;

  @JsonProperty("surname")
  private PersonMatchScoreStringParameter surname;

  @JsonProperty("dob")
  private PersonMatchScoreStringParameter dateOfBirth;

  @JsonProperty("pnc_number")
  private PersonMatchScoreStringParameter pnc;

  @JsonProperty("source_dataset")
  private PersonMatchScoreStringParameter sourceDataset;

}
