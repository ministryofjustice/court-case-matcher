package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.messaging.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.messaging.model.S3Message;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPCaseMarker;
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
import uk.gov.justice.probation.courtcasematcher.model.MessageAttribute;
import uk.gov.justice.probation.courtcasematcher.model.MessageAttributes;
import uk.gov.justice.probation.courtcasematcher.model.SnsMessageContainer;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import uk.gov.justice.probation.courtcasematcher.service.S3Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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
    private static final String EVENT_TYPE = "commonplatform.case.received";
    private static final String LARGE_EVENT_TYPE  = "commonplatform.large.case.received";

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
    @Mock
    S3Service s3Service;
    @Mock
    ObjectMapper objectMapper;
    @Mock
    private CprExtractor cprExtractor;

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
                                        .caseMarkers(Collections.singletonList(CPCaseMarker.builder()
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
                objectMapper,
                s3Service,
                LARGE_EVENT_TYPE,
                cprExtractor
        );
    }

    @Test
    void whenLibraHearingReceived_thenParseAndReturnHearing() throws JsonProcessingException {
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class))
                .thenReturn(messageContainerBuilder
                        .messageAttributes(new MessageAttributes(new MessageAttribute("String", EVENT_TYPE),
                            MessageType.LIBRA_COURT_CASE, HearingEventType.builder()
                                .value("ConfirmedOrUpdated")
                                .build()))
                        .build());
        when(libraParser.parseMessage(MESSAGE_STRING, LibraHearing.class)).thenReturn(libraHearing);

        var hearing = hearingExtractor.extractHearings(MESSAGE_CONTAINER_STRING, MESSAGE_ID);

        assertThat(hearing).isNotNull();
        assertThat(hearing.getFirst().getCaseNo()).isEqualTo(CASE_NO);
    }

    @Test
    void whenCommonPlatformHearingEventReceived_thenParseAndReturnHearing() throws JsonProcessingException {
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenReturn(messageContainerBuilder
                .messageAttributes(new MessageAttributes(new MessageAttribute("String", EVENT_TYPE),
                    MessageType.COMMON_PLATFORM_HEARING, HearingEventType.builder()
                        .value("ConfirmedOrUpdated")
                        .build()))
                .build());
        when(commonPlatformParser.parseMessage(MESSAGE_STRING, CPHearingEvent.class)).thenReturn(commonPlatformHearingEvent);

        var hearing = hearingExtractor.extractHearings(MESSAGE_CONTAINER_STRING, MESSAGE_ID);

        assertThat(hearing).isNotNull();
        assertThat(hearing.getFirst().getCaseId()).isEqualTo(CASE_ID);
        assertThat(hearing.getFirst().getHearingId()).isEqualTo(HEARING_ID);
    }

    @Test
    void whenS3StoredEventReceived_thenGetHearingFromS3_thenParseAndReturnHearing() throws JsonProcessingException {
        String s3Key = "ba8d919b-a9d8-433b-b4b4-c196f67c773e";
        String s3Bucket = "local-644707540a8083b7b15a77f51641f632";
        String messageBody = "[ \"software.amazon.payloadoffloading.PayloadS3Pointer\", {\n" +
            String.format("  \"s3BucketName\" : \"%s\",\n", s3Bucket) +
            String.format("  \"s3Key\" : \"%s\"\n", s3Key) +
            "} ]";
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenReturn(
            messageContainerBuilder
                .message(
                    messageBody
                )
            .messageAttributes(new MessageAttributes(new MessageAttribute("String", "commonplatform.large.case.received"),
                MessageType.COMMON_PLATFORM_HEARING, HearingEventType.builder()
                .value("ConfirmedOrUpdated")
                .build()))
            .build());

        ArrayList<Object> parseMessageBody = new ArrayList<>();
        parseMessageBody.add("software.amazon.payloadoffloading.PayloadS3Pointer");
        LinkedHashMap<String, String> s3Pointer = new LinkedHashMap<>();
        s3Pointer.put("s3BucketName", s3Bucket);
        s3Pointer.put("s3Key", s3Key);
        parseMessageBody.add(s3Pointer);
        when(objectMapper.readValue(messageBody, ArrayList.class)).thenReturn(parseMessageBody);
        when(objectMapper.writeValueAsString(s3Pointer)).thenReturn(s3Pointer.toString());
        when(objectMapper.readValue(s3Pointer.toString(), S3Message.class)).thenReturn(new S3Message(s3Bucket, s3Key));

        when(s3Service.getObject(s3Key)).thenReturn(MESSAGE_STRING);
        when(commonPlatformParser.parseMessage(MESSAGE_STRING, CPHearingEvent.class)).thenReturn(commonPlatformHearingEvent);

        var hearing = hearingExtractor.extractHearings(MESSAGE_CONTAINER_STRING, MESSAGE_ID);

        assertThat(hearing).isNotNull();
        assertThat(hearing.getFirst().getCaseId()).isEqualTo(CASE_ID);
        assertThat(hearing.getFirst().getHearingId()).isEqualTo(HEARING_ID);
    }

    @Test
    void whenCommonPlatformHearingEventReceived_thenParseAndReturnHearingWithCaseMarkers() throws JsonProcessingException {
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenReturn(messageContainerBuilder
                .messageAttributes(new MessageAttributes(
                    new MessageAttribute("String", EVENT_TYPE),
                    MessageType.COMMON_PLATFORM_HEARING, HearingEventType.builder()
                        .value("ConfirmedOrUpdated")
                        .build()))
                .build());
        when(commonPlatformParser.parseMessage(MESSAGE_STRING, CPHearingEvent.class)).thenReturn(commonPlatformHearingEvent);

        var hearing = hearingExtractor.extractHearings(MESSAGE_CONTAINER_STRING, MESSAGE_ID);

        assertThat(hearing).isNotNull();
        assertThat(hearing.getFirst().getCaseMarkers().size()).isEqualTo(1);
    }

    @Test
    void whenUnknownTypeReceived_thenThrow() throws JsonProcessingException {
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenReturn(messageContainerBuilder
                .messageAttributes(new MessageAttributes(new MessageAttribute("String", EVENT_TYPE),
                    MessageType.UNKNOWN, HearingEventType.builder().value("Resulted").build()))
                .build());

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> hearingExtractor.extractHearings(MESSAGE_CONTAINER_STRING, MESSAGE_ID))
                .withMessage("Unprocessable message type: UNKNOWN");
    }

    @Test
    void whenNoneTypeReceived_thenThrow() throws JsonProcessingException {
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenReturn(messageContainerBuilder
                .messageAttributes(new MessageAttributes(new MessageAttribute("String", EVENT_TYPE),
                    MessageType.NONE, HearingEventType.builder()
                        .value("Resulted")
                        .build()))
                .build());

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> hearingExtractor.extractHearings(MESSAGE_CONTAINER_STRING, MESSAGE_ID))
                .withMessage("Unprocessable message type: NONE");
    }

    @Test
    void givenInputIsInvalid_whenParsingMessageContainer_thenThrow() throws JsonProcessingException {
        final Set<? extends ConstraintViolation<?>> constraintViolations = Set.of(aViolation);
        final var violationException = new ConstraintViolationException("Validation failed", constraintViolations);
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenThrow(violationException);
        when(aViolation.getPropertyPath()).thenReturn(path);

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> hearingExtractor.extractHearings(MESSAGE_CONTAINER_STRING, MESSAGE_ID))
                .withCause(violationException)
                .withMessage("Validation failed");
    }

    @Test
    void givenInputIsInvalid_whenParsingLibraCase_thenThrow() throws JsonProcessingException {
        final var constraintViolations = Set.of(aViolation);
        final var violationException = new ConstraintViolationException("Validation failed", constraintViolations);
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenReturn(messageContainerBuilder
                .messageAttributes(new MessageAttributes(new MessageAttribute("String", EVENT_TYPE),
                    MessageType.LIBRA_COURT_CASE, HearingEventType.builder()
                        .value("Resulted")
                        .build()))
                .build());
        when(libraParser.parseMessage(MESSAGE_STRING, LibraHearing.class)).thenThrow(violationException);
        when(aViolation.getPropertyPath()).thenReturn(path);

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> hearingExtractor.extractHearings(MESSAGE_CONTAINER_STRING, MESSAGE_ID))
                .withCause(violationException)
                .withMessage("Validation failed");
    }

    @Test
    void givenJsonProcessingExceptionIsThrown_whenParsingHearing_thenThrow() throws JsonProcessingException {
        final var jsonProcessingException = new TestJsonProcessingException("💥");
        when(snsContainerParser.parseMessage(MESSAGE_CONTAINER_STRING, SnsMessageContainer.class)).thenThrow(jsonProcessingException);

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> hearingExtractor.extractHearings(MESSAGE_CONTAINER_STRING, MESSAGE_ID))
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
