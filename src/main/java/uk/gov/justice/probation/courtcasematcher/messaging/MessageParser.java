package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.Set;

@Service
@Slf4j
public class MessageParser<T> {
    private final Validator validator;
    private final ObjectMapper mapper;

    public MessageParser(final ObjectMapper mapper, final Validator validator) {
        super();
        this.mapper = mapper;
        this.validator = validator;
    }

    public T parseMessage (final String messageString, final Class<T> type) throws JsonProcessingException {
        JavaType javaType = mapper.getTypeFactory().constructType(type);
        T message = mapper.readValue(messageString, javaType);
        validate(message);
        return message;
    }

    private void validate(T messageType) {
        Set<ConstraintViolation<Object>> errors = validator.validate(messageType);
        if (!errors.isEmpty()) {
            throw new ConstraintViolationException(errors);
        }
    }

}
