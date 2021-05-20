package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import uk.gov.justice.probation.courtcasematcher.model.MessageHeader;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryEventType;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

@Slf4j
@Profile("mq-messaging")
@Component
@RequiredArgsConstructor
public class MqMessageReceiver {

    private static final String CP_QUEUE = "CP_OutboundQueue";

    private final ExternalDocumentMessageProcessor messageProcessor;

    private final TelemetryService telemetryService;

    private final MessageParser<MessageType> parser;

    @JmsListener(destination = CP_QUEUE)
    public void receive(String message) {
        log.info("Received message from JMS, queue name {}", CP_QUEUE);
        telemetryService.trackEvent(TelemetryEventType.COURT_LIST_MESSAGE_RECEIVED);
        try {
            messageProcessor.process(parse(message), null);
        } catch (Exception ex) {
            var failEvent = messageProcessor.handleException(ex, message);
            messageProcessor.logErrors(log, failEvent);
            throw new RuntimeException(failEvent.getFailureMessage(), ex);
        }
    }

    public ExternalDocumentRequest parse(String message) throws JsonProcessingException {

        MessageType messageType = parser.parseMessage(message, MessageType.class);
        MessageHeader messageHeader = messageType.getMessageHeader();
        log.info("Received message UUID {}, from {}, original timestamp {}",
            messageHeader.getMessageID().getUuid(), messageHeader.getFrom(), messageHeader.getTimeStamp());
        return messageType.getMessageBody().getGatewayOperationType().getExternalDocumentRequest();
    }

}
