package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPHearingEvent;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraHearing;
import uk.gov.justice.probation.courtcasematcher.model.SnsMessageContainer;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;

import javax.validation.ConstraintViolationException;

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

    @Value("${feature.flags.pass-hearing-id-to-court-case-service:false}")
    final boolean passHearingIdToCourtCaseService;

    Hearing extractHearing(String payload, String messageId) {
        try {
            SnsMessageContainer snsMessageContainer = snsMessageWrapperJsonParser.parseMessage(payload, SnsMessageContainer.class);
            log.debug("Extracted message ID {} from SNS message of type {}. Incoming message ID was {} ", snsMessageContainer.getMessageId(), snsMessageContainer.getMessageType(), messageId);

            return switch (snsMessageContainer.getMessageType()) {
                case LIBRA_COURT_CASE ->
                        libraParser.parseMessage(snsMessageContainer.getMessage(), LibraHearing.class).asDomain();
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

    private Hearing parseCPMessage(SnsMessageContainer snsMessageContainer) throws JsonProcessingException {
        final var cpHearingEvent = commonPlatformParser.parseMessage(snsMessageContainer.getMessage(), CPHearingEvent.class);
        final var hearing = cpHearingEvent.asDomain();
        return passHearingIdToCourtCaseService ? hearing
                .withHearingId(cpHearingEvent.getHearing().getId())
                .withHearingEventType(snsMessageContainer.getHearingEventType())
                : hearing;
    }

}
