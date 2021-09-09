package uk.gov.justice.probation.courtcasematcher.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SnsMessageContainerTest {

    @Test
    public void testGetMessageType() {
        final var commonPlatformType = SnsMessageContainer.builder()
                .messageAttributes(new MessageAttributes(MessageType.COMMON_PLATFORM_HEARING))
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

}