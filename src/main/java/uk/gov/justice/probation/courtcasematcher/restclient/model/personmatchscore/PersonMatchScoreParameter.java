package uk.gov.justice.probation.courtcasematcher.restclient.model.personmatchscore;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class PersonMatchScoreParameter<T> {
  @JsonProperty("0")
  private T value0;
  @JsonProperty("1")
  private T value1;

  public static <E> PersonMatchScoreParameter of(E value0, E value1) {
    return PersonMatchScoreParameter.builder().value0(value0).value1(value1).build();
  }
}