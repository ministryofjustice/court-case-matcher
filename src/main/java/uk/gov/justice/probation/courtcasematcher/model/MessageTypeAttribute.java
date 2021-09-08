package uk.gov.justice.probation.courtcasematcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class MessageTypeAttribute {
    @JsonProperty("Value")
    private final MessageType value;
}
