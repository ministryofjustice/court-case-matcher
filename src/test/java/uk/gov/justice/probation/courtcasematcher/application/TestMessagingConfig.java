package uk.gov.justice.probation.courtcasematcher.application;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

@Configuration
public class TestMessagingConfig {

    @Bean
    public ActiveMQConnectionFactory jmsConnectionFactory() {
        return mock(ActiveMQConnectionFactory.class);
    }

}
