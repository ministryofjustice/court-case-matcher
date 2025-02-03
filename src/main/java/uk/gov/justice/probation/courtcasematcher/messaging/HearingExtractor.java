package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.justice.probation.courtcasematcher.messaging.model.S3Message;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPHearingEvent;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraHearing;
import uk.gov.justice.probation.courtcasematcher.model.SnsMessageContainer;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;

import jakarta.validation.ConstraintViolationException;
import uk.gov.justice.probation.courtcasematcher.service.S3Service;

import java.util.ArrayList;
import java.util.List;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
@Slf4j
public class HearingExtractor {
    @NonNull
    @Autowired
    final MessageParser<SnsMessageContainer> snsMessageWrapperJsonParser;

    @NonNull
    @Autowired
    final MessageParser<LibraHearing> libraParser;

    @NonNull
    @Autowired
    final MessageParser<CPHearingEvent> commonPlatformParser;

    @NonNull
    @Autowired
    ObjectMapper objectMapper;

    @NonNull
    @Autowired
    final S3Service s3Service;

    @Value("${commonplatform.event.type.large}")
    final String largeEventType;

    @Autowired
    final CprExtractor cprExtractor;

    List<Hearing> extractHearings(String payload, String messageId) {
        try {
            SnsMessageContainer snsMessageContainer = snsMessageWrapperJsonParser.parseMessage(payload, SnsMessageContainer.class);
            log.debug("Extracted message ID {} from SNS message of type {}. Incoming message ID was {} ", snsMessageContainer.getMessageId(), snsMessageContainer.getMessageType(), messageId);

            return switch (snsMessageContainer.getMessageType()) {
                case LIBRA_COURT_CASE ->
                        List.of(libraParser.parseMessage(snsMessageContainer.getMessage(), LibraHearing.class).asDomain());
                case COMMON_PLATFORM_HEARING -> parseCPMessage(snsMessageContainer);
                default ->
                        throw new IllegalStateException("Unprocessable message type: " + snsMessageContainer.getMessageType());
            };

        } catch (ConstraintViolationException e) {
            log.error("Message validation failed. Error: {} ", e.getMessage(), e);
            e.getConstraintViolations()
                    .forEach(cv -> log.error("Validation failed : {} at {} ", cv.getMessage(), cv.getPropertyPath().toString()));
            throw new RuntimeException(e.getMessage(), e);
        } catch (JsonProcessingException e) {
            log.error("Message processing failed. Error: {} ", e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private List<Hearing> parseCPMessage(SnsMessageContainer snsMessageContainer) throws JsonProcessingException {
        String message = snsMessageContainer.getMessage();
        String eventType = snsMessageContainer.getMessageAttributes().getEventType().getValue();
        if (eventType.equals(largeEventType)) {
            message = getPayloadFromS3(snsMessageContainer);
        }
        final var cpHearingEvent = commonPlatformParser.parseMessage(message, CPHearingEvent.class);
        final var hearing = cpHearingEvent.asDomain(cprExtractor);
        return setHearingAttributes(hearing, cpHearingEvent, snsMessageContainer);
    }

    private String getPayloadFromS3(SnsMessageContainer snsMessageContainer) throws JsonProcessingException {
        var snsMessage =  objectMapper.readValue(snsMessageContainer.getMessage(), ArrayList.class);
        String s3MessageBody = objectMapper.writeValueAsString(snsMessage.get(1));
        S3Message s3Message =  objectMapper.readValue(s3MessageBody, S3Message.class);

        return s3Service.getObject(s3Message.getS3Key());
    }

    private List<Hearing> setHearingAttributes(List<Hearing> hearings, CPHearingEvent cpHearingEvent, SnsMessageContainer snsMessageContainer) {
       return hearings
            .stream()
            .map(hearing -> hearing.withHearingId(cpHearingEvent.getHearing().getId()).withHearingEventType(snsMessageContainer.getHearingEventType().getValue()))
            .toList();
    }

}
