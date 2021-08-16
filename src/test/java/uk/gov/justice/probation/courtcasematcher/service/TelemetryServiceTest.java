package uk.gov.justice.probation.courtcasematcher.service;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchResponse;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Offender;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.OffenderSearchMatchType;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.OtherIds;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.CASE_NO_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.COURT_CODE_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.COURT_ROOM_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.CRNS_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.HEARING_DATE_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.MATCHED_BY_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.MATCHES_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.PNC_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.SQS_MESSAGE_ID_KEY;

@DisplayName("Exercise TelemetryService")
@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    private static final String COURT_CODE = "B10JQ01";
    private static final String CASE_NO = "1234567890";
    private static final String CRN = "D12345";
    private static final String PNC = "PNC/123";
    private static final String COURT_ROOM = "01";
    private static final LocalDate DATE_OF_HEARING = LocalDate.of(2020, Month.NOVEMBER, 5);
    private static CourtCase courtCase;

    @Captor
    private ArgumentCaptor<Map<String, String>> propertiesCaptor;

    @Mock
    private TelemetryClient telemetryClient;

    @InjectMocks
    private TelemetryService telemetryService;

    @BeforeAll
    static void beforeEach() {

        courtCase = CourtCase.builder()
                .courtCode(COURT_CODE)
                .courtRoom(COURT_ROOM)
                .caseNo(CASE_NO)
                .pnc(PNC)
                .sessionStartTime(DATE_OF_HEARING.atStartOfDay())
                .build();
    }

    @DisplayName("Simple record of event with no properties")
    @Test
    void whenEvent_thenRecord() {
        telemetryService.trackEvent(TelemetryEventType.COURT_LIST_RECEIVED);

        verify(telemetryClient).trackEvent("PiCCourtListReceived");
    }

    @DisplayName("Record the event when an sqs message event happens")
    @Test
    void whenCaseMessageReceived_thenRecord() {
        telemetryService.trackCaseMessageReceivedEvent("messageId");

        verify(telemetryClient).trackEvent(eq("PiCCourtCaseMessageReceived"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();
        assertThat(properties).hasSize(1);
        assertThat(properties).contains(
                entry("sqsMessageId", "messageId")
        );
    }

    @DisplayName("Record the event when an sqs message event happens and the messageId is null")
    @Test
    void whenCaseMessageReceivedAndMessageIdNull_thenRecord() {
        telemetryService.trackCaseMessageReceivedEvent(null);

        verify(telemetryClient).trackEvent(eq("PiCCourtCaseMessageReceived"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();
        assertThat(properties).hasSize(0);
    }

    @DisplayName("Record the event when an exact match happens")
    @Test
    void whenExactMatch_thenRecord() {
        Match match = buildMatch(CRN);
        MatchResponse response = MatchResponse.builder()
            .matchedBy(OffenderSearchMatchType.ALL_SUPPLIED)
            .matches(List.of(match))
            .build();

        telemetryService.trackOffenderMatchEvent(courtCase, response);

        verify(telemetryClient).trackEvent(eq("PiCOffenderExactMatch"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();
        assertThat(properties).hasSize(7);
        assertThat(properties).contains(
            entry(COURT_CODE_KEY, COURT_CODE),
            entry(CASE_NO_KEY, CASE_NO),
            entry(HEARING_DATE_KEY, "2020-11-05"),
            entry(MATCHES_KEY, "1"),
            entry(MATCHED_BY_KEY, OffenderSearchMatchType.ALL_SUPPLIED.name()),
            entry(CRNS_KEY, CRN),
            entry(PNC_KEY, PNC)
        );
    }

    @DisplayName("Record the event when a partial match happens with multiple offenders")
    @Test
    void whenPartialMatchEvent_thenRecord() {
        List<Match> matches = buildMatches(List.of(CRN, "X123454"));
        MatchResponse response = MatchResponse.builder()
            .matchedBy(OffenderSearchMatchType.PARTIAL_NAME)
            .matches(matches)
            .build();

        telemetryService.trackOffenderMatchEvent(courtCase, response);

        verify(telemetryClient).trackEvent(eq("PiCOffenderPartialMatch"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();
        assertThat(properties).hasSize(7);
        assertThat(properties).contains(
            entry(COURT_CODE_KEY, COURT_CODE),
            entry(CASE_NO_KEY, CASE_NO),
            entry(HEARING_DATE_KEY, "2020-11-05"),
            entry(MATCHES_KEY, "2"),
            entry(MATCHED_BY_KEY, OffenderSearchMatchType.PARTIAL_NAME.name()),
            entry(CRNS_KEY, CRN + "," + "X123454"),
            entry(PNC_KEY, PNC)
        );
    }

    @DisplayName("Record the event when a partial match happens with a single offender")
    @Test
    void whenPartialToSingleOffenderMatchEvent_thenRecord() {
        MatchResponse response = MatchResponse.builder()
            .matchedBy(OffenderSearchMatchType.PARTIAL_NAME)
            .matches(List.of(buildMatch(CRN)))
            .build();

        telemetryService.trackOffenderMatchEvent(courtCase, response);

        verify(telemetryClient).trackEvent(eq("PiCOffenderPartialMatch"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();
        assertThat(properties).hasSize(7);
        assertThat(properties).contains(
            entry(COURT_CODE_KEY, COURT_CODE),
            entry(CASE_NO_KEY, CASE_NO),
            entry(HEARING_DATE_KEY, "2020-11-05"),
            entry(MATCHES_KEY, "1"),
            entry(MATCHED_BY_KEY, OffenderSearchMatchType.PARTIAL_NAME.name()),
            entry(CRNS_KEY, CRN),
            entry(PNC_KEY, PNC)
        );
    }

    @DisplayName("Record the event when there is no match")
    @Test
    void whenNoMatchEvent_thenRecord() {
        MatchResponse response = MatchResponse.builder()
            .matchedBy(OffenderSearchMatchType.NOTHING)
            .build();

        telemetryService.trackOffenderMatchEvent(courtCase, response);

        verify(telemetryClient).trackEvent(eq("PiCOffenderNoMatch"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();
        assertThat(properties).hasSize(4);
        assertThat(properties).contains(
            entry(COURT_CODE_KEY, COURT_CODE),
            entry(CASE_NO_KEY, CASE_NO),
            entry(HEARING_DATE_KEY, "2020-11-05"),
            entry(PNC_KEY, PNC)
        );
    }

    @DisplayName("Record the event when the call to matcher service fails")
    @Test
    void whenMatchEventFails_thenRecord() {

        telemetryService.trackOffenderMatchFailureEvent(courtCase);

        verify(telemetryClient).trackEvent(eq("PiCOffenderMatchError"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();
        assertThat(properties).hasSize(4);
        assertThat(properties).contains(
            entry(COURT_CODE_KEY, COURT_CODE),
            entry(CASE_NO_KEY, CASE_NO),
            entry(HEARING_DATE_KEY, "2020-11-05"),
            entry(PNC_KEY, PNC)
        );
    }

    @DisplayName("Record the event when a court case is received")
    @Test
    void whenCourtCaseReceived_thenRecord() {

        telemetryService.trackCourtCaseEvent(courtCase, "messageId");

        verify(telemetryClient).trackEvent(eq("PiCCourtCaseReceived"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();

        assertThat(properties).hasSize(5);
        assertThat(properties).contains(
            entry(COURT_CODE_KEY, COURT_CODE),
            entry(COURT_ROOM_KEY, COURT_ROOM),
            entry(CASE_NO_KEY, CASE_NO),
            entry(HEARING_DATE_KEY, "2020-11-05"),
            entry(SQS_MESSAGE_ID_KEY, "messageId")
        );
    }

    @DisplayName("Record the event when a court case is received and messageId is null")
    @Test
    void whenCourtCaseReceived_andMessageIdIsNull_thenRecord() {

        telemetryService.trackCourtCaseEvent(courtCase, null);

        verify(telemetryClient).trackEvent(eq("PiCCourtCaseReceived"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();

        assertThat(properties).hasSize(4);
        assertThat(properties).contains(
            entry(COURT_CODE_KEY, COURT_CODE),
            entry(COURT_ROOM_KEY, COURT_ROOM),
            entry(CASE_NO_KEY, CASE_NO),
            entry(HEARING_DATE_KEY, "2020-11-05")
        );
    }



    @DisplayName("Record the event when a court case is received as JSON and messageId is not null")
    @Test
    void whenCourtCaseReceivedFromJson_andMessageIdIsNull_thenRecord() {

        var sessionStartTime = LocalDateTime.of(DATE_OF_HEARING, LocalTime.of(9, 30, 34));
        var caseJson = CourtCase.builder().caseNo(CASE_NO).courtCode(COURT_CODE).courtRoom(COURT_ROOM)
            .sessionStartTime(sessionStartTime)
            .build();

        telemetryService.trackCourtCaseEvent(caseJson, "messageId");

        verify(telemetryClient).trackEvent(eq("PiCCourtCaseReceived"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();
        assertThat(properties).hasSize(5);
        assertThat(properties).contains(
            entry(COURT_CODE_KEY, COURT_CODE),
            entry(COURT_ROOM_KEY, COURT_ROOM),
            entry(CASE_NO_KEY, CASE_NO),
            entry(HEARING_DATE_KEY, "2020-11-05"),
            entry(SQS_MESSAGE_ID_KEY, "messageId")
        );
    }
    @Nested
    public class WithOperationTest {

        private final TelemetryContext telemetryContext = new TelemetryContext();

        @BeforeEach
        public void setUp() {
            when(telemetryClient.getContext()).thenReturn(telemetryContext);
            telemetryContext.getOperation().setId("initialValue");
        }

        @Test
        void whenWithOperationCalled_thenSetOperationId() {
            telemetryService.withOperation("operationId");
            assertThat(telemetryContext.getOperation().getId()).isEqualTo("operationId");
        }

        @Test
        void whenWithOperationCalled_andClosed_thenUnsetOperationId() throws Exception {
            final var autoCloseable = telemetryService.withOperation("operationId");
            assertThat(telemetryContext.getOperation().getId()).isEqualTo("operationId");
            autoCloseable.close();
            assertThat(telemetryContext.getOperation().getId()).isEqualTo(null);
        }
    }

    private Match buildMatch(String crn) {
        return Match.builder()
                .offender(Offender.builder()
                    .otherIds(OtherIds.builder().crn(crn).build())
                    .build())
                .build();
    }

    private List<Match> buildMatches(List<String> crns) {
        return crns.stream()
            .map(this::buildMatch)
            .collect(Collectors.toList());
    }
}
