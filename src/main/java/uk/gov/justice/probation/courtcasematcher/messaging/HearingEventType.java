package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HearingEventType {
    @JsonProperty("Value")
    private String value;
}
