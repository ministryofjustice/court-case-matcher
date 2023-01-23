package uk.gov.justice.probation.courtcasematcher.application;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.justice.probation.courtcasematcher.messaging.MessageParser;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPHearingEvent;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraHearing;
import uk.gov.justice.probation.courtcasematcher.model.SnsMessageContainer;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

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

    @Bean
    public MessageParser<LibraHearing> libraJsonParser() {
        return new MessageParser<>(objectMapper(), validator());
    }


    @Bean
    public MessageParser<CPHearingEvent> commonPlatformJsonParser() {
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
}
