package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.messaging.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPCourtCentre;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPDefendant;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPHearing;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPHearingDay;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPHearingEvent;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPLegalEntityDefendant;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPOrganisation;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPProsecutionCase;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.ProsecutionCaseIdentifier;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraHearing;
import uk.gov.justice.probation.courtcasematcher.model.MessageAttributes;
import uk.gov.justice.probation.courtcasematcher.model.SnsMessageContainer;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HearingExtractorTest {
    private static final String MESSAGE_ID = "messageId";
    private static final String MESSAGE_STRING = "messageString";
    private static final String MESSAGE_CONTAINER_STRING = "message container string";
    private static final String CASE_NO = "123456";
    private static final String CASE_ID = "26B938F7-AAE7-44EC-86FF-30DAF218B059";
    private static final String HEARING_ID = "hearing-id-one";

    @Mock
    private MessageParser<SnsMessageContainer> snsContainerParser;
    @Mock
    private MessageParser<LibraHearing> libraParser;
    @Mock
    private MessageParser<CPHearingEvent> commonPlatformParser;
    @Mock
    private ConstraintViolation<String> aViolation;
    @Mock
    private Path path;

    private HearingExtractor hearingExtractor;
    private final SnsMessageContainer.SnsMessageContainerBuilder messageContainerBuilder = SnsMessageContainer.builder()
            .message(MESSAGE_STRING);
    private final LibraHearing libraHearing = LibraHearing.builder().caseNo(CASE_NO).build();
    private final CPHearingEvent commonPlatformHearingEvent;

    {
        commonPlatformHearingEvent = CPHearingEvent.builder()
                .hearing(CPHearing.builder()
                        .id(HEARING_ID)
                        .courtCentre(CPCourtCentre.builder()
                                .code("12345")
                                .build())
                        .hearingDays(Collections.singletonList(CPHearingDay.builder().build()))
                        .prosecutionCases(Collections.singletonList(CPProsecutionCase.builder()
                                .id(CASE_ID)
                                .defendants(Collections.singletonList(CPDefendant.builder()
                                        .legalEntityDefendant(CPLegalEntityDefendant.builder()
                                                .organisation(CPOrganisation.builder().build())
                                                .build())
                                        .offences(Collections.emptyList())
                                        .build()))
                                .prosecutionCaseIdentifier(ProsecutionCaseIdentifier.builder().caseUrn("urn").build())
                                .build()))
                        .build())
                .build();
    }

    @BeforeEach
    void setUp() {
        hearingExtractor = new HearingExtractor(
                snsContainerParser,
                libraParser,
                commonPlatformParser,
                false
        );
    }

    @Test
    void whenLibraHearingReceived_thenParseAndReturnHearing() throws JsonProcessingException {
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class))
                .thenReturn(messageContainerBuilder
                        .messageAttributes(new MessageAttributes(MessageType.LIBRA_COURT_CASE, HearingEventType.builder()
                                .hearingEventTypeValue("ConfirmedOrUpdated")
                                .build()))
                        .build());
        when(libraParser.parseMessage(MESSAGE_STRING, LibraHearing.class)).thenReturn(libraHearing);

        var hearing = hearingExtractor.extractHearing(MESSAGE_CONTAINER_STRING, MESSAGE_ID);

        assertThat(hearing).isNotNull();
        assertThat(hearing.getCaseNo()).isEqualTo(CASE_NO);
    }

    @Test
    void whenCommonPlatformHearingEventReceived_thenParseAndReturnHearing() throws JsonProcessingException {
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenReturn(messageContainerBuilder
                .messageAttributes(new MessageAttributes(MessageType.COMMON_PLATFORM_HEARING, HearingEventType.builder()
                        .hearingEventTypeValue("ConfirmedOrUpdated")
                        .build()))
                .build());
        when(commonPlatformParser.parseMessage(MESSAGE_STRING, CPHearingEvent.class)).thenReturn(commonPlatformHearingEvent);

        var hearing = hearingExtractor.extractHearing(MESSAGE_CONTAINER_STRING, MESSAGE_ID);

        assertThat(hearing).isNotNull();
        assertThat(hearing.getCaseId()).isEqualTo(CASE_ID);
    }

    @Test
    void whenCommonPlatformHearingEventReceived_PassHearingIdIsFalse_thenDoNotPopulateHearingIdInHearing() throws JsonProcessingException {
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenReturn(messageContainerBuilder
                .messageAttributes(new MessageAttributes(MessageType.COMMON_PLATFORM_HEARING, HearingEventType.builder()
                        .hearingEventTypeValue("ConfirmedOrUpdated")
                        .build()))
                .build());
        when(commonPlatformParser.parseMessage(MESSAGE_STRING, CPHearingEvent.class)).thenReturn(commonPlatformHearingEvent);

        var hearing = hearingExtractor.extractHearing(MESSAGE_CONTAINER_STRING, MESSAGE_ID);

        assertThat(hearing).isNotNull();
        assertThat(hearing.getUrn()).isEqualTo("urn");
        assertThat(hearing.getCaseId()).isEqualTo(CASE_ID);
        assertThat(hearing.getHearingId()).isNull();
    }

    @Test
    void whenCommonPlatformHearingEventReceived_PassHearingIdIsTrue_thenDoNotPopulateHearingIdInHearing() throws JsonProcessingException {
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenReturn(messageContainerBuilder
                .messageAttributes(new MessageAttributes(MessageType.COMMON_PLATFORM_HEARING, HearingEventType.builder()
                        .hearingEventTypeValue("Resulted")
                        .build()))
                .build());
        when(commonPlatformParser.parseMessage(MESSAGE_STRING, CPHearingEvent.class)).thenReturn(commonPlatformHearingEvent);

        var hearing = new HearingExtractor(
                snsContainerParser,
                libraParser,
                commonPlatformParser,
                true
        ).extractHearing(MESSAGE_CONTAINER_STRING, MESSAGE_ID);

        assertThat(hearing).isNotNull();
        assertThat(hearing.getCaseId()).isEqualTo(CASE_ID);
        assertThat(hearing.getHearingId()).isEqualTo(HEARING_ID);
    }

    @Test
    void whenUnknownTypeReceived_thenThrow() throws JsonProcessingException {
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenReturn(messageContainerBuilder
                .messageAttributes(new MessageAttributes(MessageType.UNKNOWN, HearingEventType.builder()
                        .hearingEventTypeValue("Resulted")
                        .build()))
                .build());

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> hearingExtractor.extractHearing(MESSAGE_CONTAINER_STRING, MESSAGE_ID))
                .withMessage("Unprocessable message type: UNKNOWN");
    }

    @Test
    void whenNoneTypeReceived_thenThrow() throws JsonProcessingException {
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenReturn(messageContainerBuilder
                .messageAttributes(new MessageAttributes(MessageType.NONE, HearingEventType.builder()
                        .hearingEventTypeValue("Resulted")
                        .build()))
                .build());

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> hearingExtractor.extractHearing(MESSAGE_CONTAINER_STRING, MESSAGE_ID))
                .withMessage("Unprocessable message type: NONE");
    }

    @Test
    void givenInputIsInvalid_whenParsingMessageContainer_thenThrow() throws JsonProcessingException {
        final Set<? extends ConstraintViolation<?>> constraintViolations = Set.of(aViolation);
        final var violationException = new ConstraintViolationException("Validation failed", constraintViolations);
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenThrow(violationException);
        when(aViolation.getPropertyPath()).thenReturn(path);

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> hearingExtractor.extractHearing(MESSAGE_CONTAINER_STRING, MESSAGE_ID))
                .withCause(violationException)
                .withMessage("Validation failed");
    }

    @Test
    void givenInputIsInvalid_whenParsingLibraCase_thenThrow() throws JsonProcessingException {
        final var constraintViolations = Set.of(aViolation);
        final var violationException = new ConstraintViolationException("Validation failed", constraintViolations);
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenReturn(messageContainerBuilder
                .messageAttributes(new MessageAttributes(MessageType.LIBRA_COURT_CASE, HearingEventType.builder()
                        .hearingEventTypeValue("Resulted")
                        .build()))
                .build());
        when(libraParser.parseMessage(MESSAGE_STRING, LibraHearing.class)).thenThrow(violationException);
        when(aViolation.getPropertyPath()).thenReturn(path);

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> hearingExtractor.extractHearing(MESSAGE_CONTAINER_STRING, MESSAGE_ID))
                .withCause(violationException)
                .withMessage("Validation failed");
    }

    @Test
    void givenJsonProcessingExceptionIsThrown_whenParsingHearing_thenThrow() throws JsonProcessingException {
        final var jsonProcessingException = new TestJsonProcessingException("💥");
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenThrow(jsonProcessingException);

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> hearingExtractor.extractHearing(MESSAGE_CONTAINER_STRING, MESSAGE_ID))
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
