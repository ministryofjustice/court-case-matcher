package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraCase;
import uk.gov.justice.probation.courtcasematcher.model.SnsMessageContainer;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
@Slf4j
public class CaseExtractor {
    @Autowired
    @Qualifier("snsMessageWrapperJsonParser")
    final MessageParser<SnsMessageContainer> snsMessageWrapperJsonParser;

    @Autowired
    @Qualifier("caseJsonParser")
    final MessageParser<LibraCase> parser;


    CourtCase extractCourtCase(String payload, String messageId) throws JsonProcessingException {
        var snsMessageContainer = snsMessageWrapperJsonParser.parseMessage(payload, SnsMessageContainer.class);
        log.debug("Extracted message ID {} from SNS message of type {}. Incoming message ID was {} ", snsMessageContainer.getMessageId(), snsMessageContainer.getMessageType(), messageId);

        return parser.parseMessage(snsMessageContainer.getMessage(), LibraCase.class).asDomain();
    }

}
