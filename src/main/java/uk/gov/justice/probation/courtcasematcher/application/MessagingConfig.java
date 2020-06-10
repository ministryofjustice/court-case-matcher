package uk.gov.justice.probation.courtcasematcher.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.eventbus.EventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MessagingConfig {

    // Without this, Spring uses the XmlMapper bean as the ObjectMapper for the whole app and we get actuator response as XML
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean(name = "gatewayMessageXmlMapper")
    public XmlMapper xmlMapper() {
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        XmlMapper mapper = new XmlMapper(xmlModule);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public EventBus eventBus() {
        return new EventBus();
    }

//    @Bean
//    public ActiveMQConnectionFactory connectionFactory(@Value("${spring.activemq.brokerUrl}") String brokerUrl) {
//        return new ActiveMQConnectionFactory(brokerUrl, "jmsuser", "jmsuser");
//    }

//    @Bean
//    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
//                                                DefaultJmsListenerContainerFactoryConfigurer configurer,
//        ActiveMQConnectionFactory connectionFactory,
//                                                JmsGlobalErrorHandler myErrorHandler) {
//
//        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
//        configurer.configure(factory, connectionFactory);
//        factory.setErrorHandler(myErrorHandler);
//        return factory;
//    }

//    @Bean
//    public JmsListenerContainerFactory<?> myFactory(
////        ConnectionFactory connectionFactory,
//        DefaultJmsListenerContainerFactoryConfigurer configurer,
//        JmsGlobalErrorHandler errorHandler) {
//        ActiveMQConnectionFactory activeMQConnectionFactory = (ActiveMQConnectionFactory)connectionFactory;
////        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
////        redeliveryPolicy.setMaximumRedeliveries(1);
////        activeMQConnectionFactory.set
////        activeMQConnectionFactory.setRedeliveryPolicy(redeliveryPolicy);
//
//        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
//        factory.setErrorHandler(errorHandler);
//        configurer.configure(factory, activeMQConnectionFactory);
//        return factory;
//    }
}
