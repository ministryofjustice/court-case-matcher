package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.time.LocalDateTime;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.application.CaseMapperReference;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.DocumentWrapper;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;

@Service
@Slf4j
public class GatewayMessageParser {

    private final XmlMapper xmlMapper;
    private final CaseMapperReference caseMapperReference;

    public static final String EXT_DOC_NS = "http://www.justice.gov.uk/magistrates/external/ExternalDocumentRequest";
    public static final String CSCI_HDR_NS = "http://www.justice.gov.uk/magistrates/generic/CSCI_Header";
    public static final String CSCI_BODY_NS = "http://www.justice.gov.uk/magistrates/cp/CSCI_Body";
    public static final String CSC_STATUS_NS = "http://www.justice.gov.uk/magistrates/generic/CSCI_Status";
    public static final String GW_MSG_SCHEMA = "http://www.justice.gov.uk/magistrates/cp/GatewayMessageSchema";

    public GatewayMessageParser(@Qualifier("gatewayMessageXmlMapper") XmlMapper xmlMapper, @Autowired CaseMapperReference caseMapperReference) {
        super();
        this.xmlMapper = xmlMapper;
        this.caseMapperReference = caseMapperReference;
    }

    public MessageType parseMessage (String xml) throws JsonProcessingException {
        final MessageType messageType = xmlMapper.readValue(xml, MessageType.class);
        linkCaseToSession(messageType.getMessageBody().getGatewayOperationType().getExternalDocumentRequest().getDocumentWrapper());
        return messageType;
    }

    private void linkCaseToSession(DocumentWrapper documentWrapper) {
        documentWrapper.getDocument().stream()
            .map(document -> document.getData().getJob().getSessions())
            .flatMap(Collection::stream)
            .forEach(this::collectBlockCasesForSessions);
    }

    private void collectBlockCasesForSessions(Session session) {
        session.getBlocks().stream()
            .flatMap(block -> block.getCases().stream())
            .forEach(aCase -> applySessionFieldsToCase(aCase, session));
    }

    private void applySessionFieldsToCase(Case aCase, Session session) {
        aCase.setCourtCode(caseMapperReference.getCourtCodeFromName(session.getCourt()));
        aCase.setCourtRoom(session.getRoom());
        aCase.setSessionStartTime(LocalDateTime.of(session.getDateOfHearing(), session.getStart()));
    }

}
