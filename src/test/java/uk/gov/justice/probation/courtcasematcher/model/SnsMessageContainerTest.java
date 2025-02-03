package uk.gov.justice.probation.courtcasematcher.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.justice.probation.courtcasematcher.messaging.HearingEventType;
import uk.gov.justice.probation.courtcasematcher.messaging.model.MessageType;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SnsMessageContainerTest {

    @Value("${commonplatform.event.type.default}")
    String eventType;

    @Test
    public void testGetMessageType() {
        final var commonPlatformType = SnsMessageContainer.builder()
                .messageAttributes(new MessageAttributes(new MessageAttribute("String", eventType),
                    MessageType.COMMON_PLATFORM_HEARING, HearingEventType.builder()
                        .value("Resulted")
                        .build()))
                .build();
        assertThat(commonPlatformType.getMessageType()).isEqualTo(MessageType.COMMON_PLATFORM_HEARING);
    }

    @Test
    public void testGetNullMessageType() {
        final var commonPlatformType = SnsMessageContainer.builder()
                .messageAttributes(null)
                .build();
        assertThat(commonPlatformType.getMessageType()).isEqualTo(MessageType.NONE);
    }

    @Test
    public void testGetHearingEventType() {
        final var commonPlatformType = SnsMessageContainer.builder()
                .messageAttributes(new MessageAttributes(new MessageAttribute("String", eventType),
                    MessageType.COMMON_PLATFORM_HEARING, HearingEventType.builder()
                        .value("Resulted")
                        .build()))
                .build();
        assertThat(commonPlatformType.getHearingEventType()).isEqualTo(HearingEventType.builder()
                .value("Resulted")
                .build());
    }

}
