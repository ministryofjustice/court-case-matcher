package uk.gov.justice.probation.courtcasematcher.application;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.eventbus.EventBus;
import javax.validation.Validation;
import javax.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.justice.probation.courtcasematcher.messaging.MessageParser;
import uk.gov.justice.probation.courtcasematcher.model.SnsMessageContainer;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;

@Configuration
public class MessagingConfig {

    private void configureMapper(ObjectMapper objectMapper) {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
    }

    // Without this, Spring uses the XmlMapper bean as the ObjectMapper for the whole app and we get actuator response as XML
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        var objectMapper = new ObjectMapper();
        configureMapper(objectMapper);
        return objectMapper;
    }

    @Bean(name = "externalDocumentXmlParser")
    public MessageParser<ExternalDocumentRequest> externalDocumentXmlParser() {
        var xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        var mapper = new XmlMapper(xmlModule);
        configureMapper(mapper);
        return new MessageParser<>(mapper, validator());
    }

    @Bean(name = "caseJsonParser")
    public MessageParser<Case> caseJsonParser() {
        return new MessageParser<>(objectMapper(), validator());
    }

    @Bean(name = "snsMessageWrapperJsonParser")
    public MessageParser<SnsMessageContainer> snsMessageWrapperJsonParser() {
        return new MessageParser<>(objectMapper(), validator());
    }

    @Bean
    public Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Bean
    public EventBus eventBus() {
        return new EventBus();
    }

}
