package uk.gov.justice.probation.courtcasematcher.application;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestMessagingConfig {

    @Bean
    @Primary
    @Qualifier("jmsConnectionFactory")
    public ActiveMQConnectionFactory jmsConnectionFactory() {
        return mock(ActiveMQConnectionFactory.class);
    }

}
