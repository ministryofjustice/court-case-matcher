package uk.gov.justice.probation.courtcasematcher.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MessageReceiver {

    private final MessageProcessor messageProcessor;

    public MessageReceiver (MessageProcessor processor) {
        super();
        this.messageProcessor = processor;
    }

    @JmsListener(destination = "CP_OutboundQueue")
    public void receive(String message) {
        log.info("Received message");
        messageProcessor.process(message);
    }
}
