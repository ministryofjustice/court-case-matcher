package uk.gov.justice.probation.courtcasematcher.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.messaging.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.type.HearingEventType;

@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class MessageAttributes {
    private final MessageType messageType;
    private final HearingEventType hearingEventType;
}
