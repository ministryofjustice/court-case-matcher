package uk.gov.justice.probation.courtcasematcher.messaging;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class MockTestConfiguration {
    @Bean
    @Primary
    public MessageReceiver messageReceiver() {
        return Mockito.mock(MessageReceiver.class);
    }
}
