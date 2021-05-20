package uk.gov.justice.probation.courtcasematcher.messaging;

import java.util.Set;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MessageParser<T> {

    public static final String EXT_DOC_NS = "http://www.justice.gov.uk/magistrates/external/ExternalDocumentRequest";
    public static final String CSCI_HDR_NS = "http://www.justice.gov.uk/magistrates/generic/CSCI_Header";
    public static final String CSCI_BODY_NS = "http://www.justice.gov.uk/magistrates/cp/CSCI_Body";
    public static final String CSC_STATUS_NS = "http://www.justice.gov.uk/magistrates/generic/CSCI_Status";
    public static final String GW_MSG_SCHEMA = "http://www.justice.gov.uk/magistrates/cp/GatewayMessageSchema";

    private final Validator validator;
    private final ObjectMapper mapper;

    public MessageParser(final ObjectMapper mapper, final Validator validator) {
        super();
        this.mapper = mapper;
        this.validator = validator;
    }

    public T parseMessage (final String xml, final Class<T> type) throws JsonProcessingException {
        JavaType javaType = mapper.getTypeFactory().constructType(type);
        T message = mapper.readValue(xml, javaType);
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
