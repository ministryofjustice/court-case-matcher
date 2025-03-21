package uk.gov.justice.probation.courtcasematcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.messaging.HearingEventType;
import uk.gov.justice.probation.courtcasematcher.messaging.model.MessageType;

@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class MessageAttributes {
    @JsonProperty("eventType")
    private final MessageAttribute eventType;

    @JsonProperty("messageType")
    private final MessageType messageType;

    @JsonProperty("hearingEventType")
    private final HearingEventType hearingEventType;
}
