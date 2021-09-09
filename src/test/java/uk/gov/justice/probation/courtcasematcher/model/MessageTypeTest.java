package uk.gov.justice.probation.courtcasematcher.model;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.messaging.model.MessageType;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class MessageTypeTest {

    @Test
    public void testCreator() {
        assertThat(MessageType.of("COMMON_PLATFORM_HEARING")).isEqualTo(MessageType.COMMON_PLATFORM_HEARING);
        assertThat(MessageType.of("CP_TEST_COURT_CASE")).isEqualTo(MessageType.COMMON_PLATFORM_HEARING);
        assertThat(MessageType.of("LIBRA_COURT_CASE")).isEqualTo(MessageType.LIBRA_COURT_CASE);
        assertThat(MessageType.of("SUMMAT_WRONG")).isEqualTo(MessageType.UNKNOWN);
        assertThat(MessageType.of(null)).isEqualTo(MessageType.NONE);
    }

}
