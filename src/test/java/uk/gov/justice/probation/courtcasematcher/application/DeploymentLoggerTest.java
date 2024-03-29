package uk.gov.justice.probation.courtcasematcher.application;

import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.BuildProperties;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeploymentLoggerTest {
    @Mock
    private BuildProperties buildProperties;

    @Test
    public void shouldLogInfo() throws UnknownHostException {
        when(buildProperties.getVersion()).thenReturn("expected_version");
        when(buildProperties.getName()).thenReturn("court-case-matcher");
        final var logger = new DeploymentLogger(buildProperties);

        TestAppender.events.clear();
        logger.onApplicationEvent(null);
        assertThat(TestAppender.events.size()).isEqualTo(1);
        assertThat(TestAppender.events.get(0).toString()).startsWith("[INFO] Starting court-case-matcher expected_version using Java ");
    }
}
