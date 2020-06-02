package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.eventbus.EventBus;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.model.cp.csci.CSCIMessageType;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.model.generic.csci_header.MessageHeader;

@Service
@Slf4j
public class MessageProcessor {

    @SuppressWarnings("UnstableApiUsage") // Not part of the final product
    private final EventBus eventBus;

    private final GatewayMessageParser parser;

    public MessageProcessor(GatewayMessageParser parser, EventBus eventBus) {
        this.eventBus = eventBus;
        this.parser = parser;
    }

    public void process(String message) {
        parse(message).ifPresent(this::process);
    }

    private Optional<List<Session>> parse(String message) {

        CSCIMessageType csciMessageType;
        try {
            csciMessageType = parser.parseMessage(message);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse message", e);
            return Optional.empty();
        }

        logMessageReceipt(csciMessageType.getMessageHeader());

        List<Session> sessions = csciMessageType.getMessageBody()
            .getGatewayOperationType()
            .getExternalDocumentRequest()
            .getDocumentWrapper()
            .getDocument()
            .stream()
            .flatMap(document -> document.getData().getJob().getSessions().stream())
            .collect(Collectors.toList());

        return Optional.of(sessions);
    }

    private void process(List<Session> sessions) {
        List<Case> cases = sessions
            .stream()
            .flatMap(session -> session.getBlocks().stream())
            .flatMap(block -> block.getCases().stream())
            .collect(Collectors.toList());

        log.info("Received {} cases", cases.size());

        cases.forEach(this::store);
    }

    private void store(Case aCase) {
        eventBus.post(aCase);
        log.info("Successfully published case ID: {}, case no:{} for defendant: {}", aCase.getId(), aCase.getCaseNo(), aCase.getDef_name());
    }

    private void logMessageReceipt(MessageHeader messageHeader) {
        log.info("Received message UUID {}, from {}, original timestamp {}",
            messageHeader.getMessageID().getUuid(), messageHeader.getFrom(), messageHeader.getTimeStamp());
    }
}
