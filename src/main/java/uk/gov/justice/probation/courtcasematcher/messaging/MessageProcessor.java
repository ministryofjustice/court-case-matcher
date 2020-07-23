package uk.gov.justice.probation.courtcasematcher.messaging;

import com.google.common.eventbus.EventBus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.model.MessageHeader;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.DocumentWrapper;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;

@Service
@Slf4j
public class MessageProcessor {

    @SuppressWarnings("UnstableApiUsage") // Not part of the final product
    private final EventBus eventBus;

    private final GatewayMessageParser parser;

    private final MatcherService matcherService;

    private final CourtCaseService courtCaseService;

    @Autowired
    public MessageProcessor(GatewayMessageParser gatewayMessageParser, EventBus eventBus, MatcherService matcherService, CourtCaseService courtCaseService) {
        super();
        this.parser = gatewayMessageParser;
        this.eventBus = eventBus;
        this.matcherService = matcherService;
        this.courtCaseService = courtCaseService;
    }

    public void process(String message) {
        parse(message).ifPresent(this::process);
    }

    private Optional<MessageType> parse(String message) {

        MessageType messageType;
        try {
            messageType = parser.parseMessage(message);
        }
        catch (ConstraintViolationException | IOException ex) {
            log.error("Failed to parse and validate message", ex);
            CourtCaseFailureEvent.CourtCaseFailureEventBuilder builder = CourtCaseFailureEvent.builder()
                .failureMessage(ex.getMessage())
                .incomingMessage(message);
            if (ex instanceof ConstraintViolationException) {
                builder.violations(((ConstraintViolationException)ex).getConstraintViolations());
            }
            eventBus.post(builder.build());
            return Optional.empty();
        }
        logMessageReceipt(messageType.getMessageHeader());

        return Optional.of(messageType);
    }

    private void process(MessageType messageType) {
        DocumentWrapper documentWrapper = messageType.getMessageBody().getGatewayOperationType().getExternalDocumentRequest().getDocumentWrapper();
        List<Session> sessions = documentWrapper
            .getDocument()
            .stream()
            .flatMap(document -> document.getData().getJob().getSessions().stream())
            .collect(Collectors.toList());

        List<CompletableFuture<Long>> futures = matchCases(sessions);

        // This is a temporary measure to get the court code until we have proper court reference data
        String courtCode = getCourtCode(sessions);
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() -> {
            log.debug("Purging cases for court {}", courtCode);
            courtCaseService.purgeAbsent(courtCode, getAllCases(sessions));
        });
    }

    private List<CompletableFuture<Long>> matchCases(List<Session> sessions) {

        List<CompletableFuture<Long>> allFutures = new ArrayList<>();
        sessions.forEach(session -> {
            List<Case> cases = session.getBlocks().stream()
                .flatMap(block -> block.getCases().stream())
                .collect(Collectors.toList());
            allFutures.add(CompletableFuture.supplyAsync(() -> matchCases(session, cases)));
        });
        return allFutures;
    }

    private Long matchCases(Session session, List<Case> cases) {
        log.debug("Matching {} cases for court {}, session {}", cases.size(), session.getCourtCode(), session.getId());
        cases.forEach(matcherService::match);
        return session.getId();
    }

    private String getCourtCode(List<Session> sessions) {
        Set<String> courtCodes = sessions.stream()
            .map(Session::getCourtCode)
            .collect(Collectors.toSet());
        if (courtCodes.size() > 1) {
            log.warn("Unexpected multiple court codes. Count was {}, elements {}", courtCodes.size(), courtCodes);
        }
        return courtCodes.iterator().next();
    }

    private List<Case> getAllCases(List<Session> sessions) {
        return sessions.stream()
            .flatMap(session -> session.getBlocks().stream())
            .flatMap(block -> block.getCases().stream())
            .collect(Collectors.toList());
    }

    private void logMessageReceipt(MessageHeader messageHeader) {
        log.info("Received message UUID {}, from {}, original timestamp {}",
            messageHeader.getMessageID().getUuid(), messageHeader.getFrom(), messageHeader.getTimeStamp());
    }
}
