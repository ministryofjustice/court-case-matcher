package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.messaging.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CommonPlatformHearing;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraCase;
import uk.gov.justice.probation.courtcasematcher.model.MessageAttributes;
import uk.gov.justice.probation.courtcasematcher.model.SnsMessageContainer;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtCaseExtractorTest {
    private static final String MESSAGE_ID = "messageId";
    private static final String MESSAGE_STRING = "messageString";
    private static final String MESSAGE_CONTAINER_STRING = "message container string";
    private static final String CASE_NO = "123456";
    private static final String CASE_ID = "26B938F7-AAE7-44EC-86FF-30DAF218B059";

    @Mock
    private MessageParser<SnsMessageContainer> snsContainerParser;
    @Mock
    private MessageParser<LibraCase> libraParser;
    @Mock
    private MessageParser<CommonPlatformHearing> commonPlatformParser;
    @Mock
    private ConstraintViolation<String> aViolation;
    @Mock
    private Path path;

    private CourtCaseExtractor caseExtractor;
    private final SnsMessageContainer.SnsMessageContainerBuilder messageContainerBuilder = SnsMessageContainer.builder()
            .message(MESSAGE_STRING);
    private final LibraCase libraCase = LibraCase.builder().caseNo(CASE_NO).build();
    private final CommonPlatformHearing commonPlatformHearing = CommonPlatformHearing.builder().id(CASE_ID).build();

    @BeforeEach
    void setUp() {
        caseExtractor = new CourtCaseExtractor(
                snsContainerParser,
                libraParser,
                commonPlatformParser
        );
    }

    @Test
    void whenLibraCaseReceived_thenParseAndReturnCase() throws JsonProcessingException {
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class))
                .thenReturn(messageContainerBuilder
                        .messageAttributes(new MessageAttributes(MessageType.LIBRA_COURT_CASE))
                        .build());
        when(libraParser.parseMessage(MESSAGE_STRING, LibraCase.class)).thenReturn(libraCase);

        var courtCase = caseExtractor.extractCourtCase(MESSAGE_CONTAINER_STRING, MESSAGE_ID);

        assertThat(courtCase).isNotNull();
        assertThat(courtCase.getCaseNo()).isEqualTo(CASE_NO);
    }

    @Test
    void whenCommonPlatformHearingReceived_thenParseAndReturnCase() throws JsonProcessingException {
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenReturn(messageContainerBuilder
                .messageAttributes(new MessageAttributes(MessageType.COMMON_PLATFORM_HEARING))
                .build());
        when(commonPlatformParser.parseMessage(MESSAGE_STRING, CommonPlatformHearing.class)).thenReturn(commonPlatformHearing);

        var courtCase = caseExtractor.extractCourtCase(MESSAGE_CONTAINER_STRING, MESSAGE_ID);

        assertThat(courtCase).isNotNull();
        assertThat(courtCase.getCaseId()).isEqualTo(CASE_ID);
    }

    @Test
    void whenUnknownTypeReceived_thenThrow() throws JsonProcessingException {
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenReturn(messageContainerBuilder
                .messageAttributes(new MessageAttributes(MessageType.UNKNOWN))
                .build());

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> caseExtractor.extractCourtCase(MESSAGE_CONTAINER_STRING, MESSAGE_ID))
                .withMessage("Unprocessable message type: UNKNOWN");
    }

    @Test
    void whenNoneTypeReceived_thenThrow() throws JsonProcessingException {
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenReturn(messageContainerBuilder
                .messageAttributes(new MessageAttributes(MessageType.NONE))
                .build());

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> caseExtractor.extractCourtCase(MESSAGE_CONTAINER_STRING, MESSAGE_ID))
                .withMessage("Unprocessable message type: NONE");
    }

    @Test
    void givenInputIsInvalid_whenParsingMessageContainer_thenThrow() throws JsonProcessingException {
        final Set<? extends ConstraintViolation<?>> constraintViolations = Set.of(aViolation);
        final var violationException = new ConstraintViolationException("Validation failed", constraintViolations);
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenThrow(violationException);
        when(aViolation.getPropertyPath()).thenReturn(path);

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> caseExtractor.extractCourtCase(MESSAGE_CONTAINER_STRING, MESSAGE_ID))
                .withCause(violationException)
                .withMessage("Validation failed");
    }

    @Test
    void givenInputIsInvalid_whenParsingLibraCase_thenThrow() throws JsonProcessingException {
        final var constraintViolations = Set.of(aViolation);
        final var violationException = new ConstraintViolationException("Validation failed", constraintViolations);
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenReturn(messageContainerBuilder
                .messageAttributes(new MessageAttributes(MessageType.LIBRA_COURT_CASE))
                .build());
        when(libraParser.parseMessage(MESSAGE_STRING, LibraCase.class)).thenThrow(violationException);
        when(aViolation.getPropertyPath()).thenReturn(path);

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> caseExtractor.extractCourtCase(MESSAGE_CONTAINER_STRING, MESSAGE_ID))
                .withCause(violationException)
                .withMessage("Validation failed");
    }

    @Test
    void givenJsonProcessingExceptionIsThrown_whenParsingCase_thenThrow() throws JsonProcessingException {
        final var jsonProcessingException = new TestJsonProcessingException("💥");
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenThrow(jsonProcessingException);

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> caseExtractor.extractCourtCase(MESSAGE_CONTAINER_STRING, MESSAGE_ID))
                .withCause(jsonProcessingException)
                .withMessage("💥");
    }

    // JsonProcessingException is protected so we need to subclass it to test
    static class TestJsonProcessingException extends JsonProcessingException {
        protected TestJsonProcessingException(String msg) {
            super(msg);
        }
    }
}
