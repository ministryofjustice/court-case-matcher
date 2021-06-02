package uk.gov.justice.probation.courtcasematcher.messaging;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.google.common.eventbus.EventBus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseMatchEvent;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseUpdateEvent;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Document;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.DocumentWrapper;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Info;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Service
@Qualifier("xmlMessageProcessor")
@Slf4j
public class ExternalDocumentMessageProcessor implements MessageProcessor {

    @Autowired
    @SuppressWarnings("UnstableApiUsage")
    private final EventBus eventBus;

    @Autowired
    private final TelemetryService telemetryService;

    @Autowired
    private final CourtCaseService courtCaseService;

    @Autowired
    @Qualifier("externalDocumentXmlParser")
    private final MessageParser<ExternalDocumentRequest> parser;

    @Override
    public void process(String payload, String messageId) {

        ExternalDocumentRequest externalDocumentRequest;
        try {
            externalDocumentRequest = parser.parseMessage(payload, ExternalDocumentRequest.class);
        } catch (Exception ex) {
            var failEvent = handleException(ex, payload);
            logErrors(log, failEvent);
            throw new RuntimeException(failEvent.getFailureMessage(), ex);
        }

        process(externalDocumentRequest, messageId);
    }

    public void process(ExternalDocumentRequest externalDocumentRequest, String messageId) {

        var documents = extractDocuments(externalDocumentRequest);

        documents.stream()
            .map(Document::getInfo)
            .distinct()
            .forEach(info -> trackCourtListReceipt(info, messageId));

        var sessions = documents
            .stream()
            .flatMap(document -> document.getData().getJob().getSessions().stream())
            .collect(Collectors.toList());

        saveCases(sessions, messageId);
    }

    private void saveCases(List<Session> sessions, String messageId) {
        sessions.forEach(session -> {
            log.debug("Starting to process cases in session court {}, room {}, date {}",
                session.getCourtCode(), session.getCourtRoom(), session.getDateOfHearing());

            List<String> cases = session.getBlocks().stream()
                .flatMap(block -> block.getCases().stream())
                .map(aCase -> saveCase(aCase, messageId))
                .collect(Collectors.toList());
            log.debug("Completed {} cases for {}, {}, {}", cases.size(), session.getCourtCode(), session.getCourtRoom(), session.getDateOfHearing());
        });
    }

    @Override
    public void postCaseEvent(CourtCase courtCase) {
        if (courtCase.shouldMatchToOffender()) {
            eventBus.post(new CourtCaseMatchEvent(courtCase));
        }
        else {
            eventBus.post(new CourtCaseUpdateEvent(courtCase));
        }
    }

    private void trackCourtListReceipt(Info info, String messageId) {
        log.debug("Received court list for court {} on {}, message ID {}", info.getOuCode(), info.getDateOfHearing().toString(), messageId);
    }

    private List<Document> extractDocuments(ExternalDocumentRequest externalDocumentRequest) {
        return Optional.ofNullable(externalDocumentRequest.getDocumentWrapper())
            .map(DocumentWrapper::getDocument)
            .or(() -> Optional.of(Collections.emptyList()))
            .orElse(Collections.emptyList());
    }

    @Override
    public TelemetryService getTelemetryService() {
        return telemetryService;
    }

    @Override
    public CourtCaseService getCourtCaseService() {
        return courtCaseService;
    }
}
