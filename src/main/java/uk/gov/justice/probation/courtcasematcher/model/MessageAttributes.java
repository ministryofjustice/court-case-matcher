package uk.gov.justice.probation.courtcasematcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.messaging.HearingEventType;
import uk.gov.justice.probation.courtcasematcher.messaging.model.MessageType;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class MessageAttributes  implements Serializable {
    @JsonProperty("messageType")
    private final MessageType messageType;

    @JsonProperty("hearingEventType")
    private final HearingEventType hearingEventType;

    @JsonProperty("ExtendedPayloadSize")
    private final ExtendedPayloadSize extendedPayloadSize;
}
