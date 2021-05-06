package uk.gov.justice.probation.courtcasematcher.application;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.boot.actuate.jms.JmsHealthIndicator;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import uk.gov.justice.probation.courtcasematcher.application.healthchecks.SqsCheck;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestMessagingConfig {

    @Bean
    public ActiveMQConnectionFactory jmsConnectionFactory() {
        return mock(ActiveMQConnectionFactory.class);
    }

    @Bean
    public JmsHealthIndicator jmsHealthIndicator() {
        return mock(JmsHealthIndicator.class);
    }

    @MockBean
    public SqsCheck sqsCheck;

    @MockBean
    private BuildProperties buildProperties;

}
