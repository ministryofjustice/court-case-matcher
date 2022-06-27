package uk.gov.justice.probation.courtcasematcher.service;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import joptsimple.internal.Strings;
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
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.Offender;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OffenderSearchMatchType;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OtherIds;

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
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.*;

@DisplayName("Exercise TelemetryService")
@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    private static final String COURT_CODE = "B10JQ01";
    private static final String CASE_NO = "1234567890";
    private static final String CASE_ID = "D517D32D-3C80-41E8-846E-D274DC2B94A5";
    private static final String CRN = "D12345";
    private static final String PNC = "PNC/123";
    private static final String PNC2 = "PNC/456";

    private static final String DEFENDANT_ID_ONE = "defendant-id-one";
    private static final String DEFENDANT_ID_TWO = "defendant-id-two";

    private static final String URN = "URN/123";

    private static final String HEARING_ID = "H123RT34";
    private static final Defendant DEFENDANT = Defendant.builder()
            .pnc(PNC)
            .build();
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
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(COURT_CODE)
                        .courtRoom(COURT_ROOM)
                        .sessionStartTime(DATE_OF_HEARING.atStartOfDay())
                        .build()))
                .caseNo(CASE_NO)
                .urn(URN)
                .hearingId(HEARING_ID)
                .defendants(List.of(Defendant.builder()
                    .defendantId(DEFENDANT_ID_ONE)
                        .pnc(PNC)
                        .build(),
                  Defendant.builder()
                    .defendantId(DEFENDANT_ID_TWO)
                        .pnc(PNC2)
                        .build()))
                .source(DataSource.COMMON_PLATFORM)
                .caseId(CASE_ID)
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

        telemetryService.trackOffenderMatchEvent(DEFENDANT, courtCase, response);

        verify(telemetryClient).trackEvent(eq("PiCOffenderExactMatch"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();
        assertCourtCaseProperties(14);
        assertThat(properties).contains(
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

        telemetryService.trackOffenderMatchEvent(DEFENDANT, courtCase, response);

        verify(telemetryClient).trackEvent(eq("PiCOffenderPartialMatch"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        assertCourtCaseProperties(14);
        assertThat(propertiesCaptor.getValue()).contains(
                entry(MATCHES_KEY, "2"),
                entry(MATCHED_BY_KEY, OffenderSearchMatchType.PARTIAL_NAME.name()),
                entry(CRNS_KEY, CRN + "," + "X123454")
        );
    }

    @DisplayName("Record the event when a partial match happens with a single offender")
    @Test
    void whenPartialToSingleOffenderMatchEvent_thenRecord() {
        MatchResponse response = MatchResponse.builder()
                .matchedBy(OffenderSearchMatchType.PARTIAL_NAME)
                .matches(List.of(buildMatch(CRN)))
                .build();

        telemetryService.trackOffenderMatchEvent(DEFENDANT, courtCase, response);

        verify(telemetryClient).trackEvent(eq("PiCOffenderPartialMatch"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();
        assertCourtCaseProperties(14);
        assertThat(properties).contains(
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

        telemetryService.trackOffenderMatchEvent(DEFENDANT, courtCase, response);

        verify(telemetryClient).trackEvent(eq("PiCOffenderNoMatch"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        assertCourtCaseProperties(10);
        assertThat(propertiesCaptor.getValue()).contains(
                entry(PNC_KEY, PNC)
        );
    }

    @DisplayName("Record the event when the call to matcher service fails")
    @Test
    void whenMatchEventFails_thenRecord() {

        final Defendant defendant = Defendant.builder()
                .pnc(PNC)
                .build();
        telemetryService.trackOffenderMatchFailureEvent(defendant, courtCase);

        verify(telemetryClient).trackEvent(eq("PiCOffenderMatchError"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        assertCourtCaseProperties(10);
        assertThat(propertiesCaptor.getValue()).contains(
                entry(PNC_KEY, PNC)
        );
    }

    @DisplayName("Record the event when a court case is received")
    @Test
    void whenCourtCaseReceived_thenRecord() {

        telemetryService.trackCourtCaseEvent(courtCase, "messageId");

        verify(telemetryClient).trackEvent(eq("PiCCourtCaseReceived"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();

        assertCourtCaseProperties(10);
        assertThat(properties).contains(
                entry(SQS_MESSAGE_ID_KEY, "messageId")
        );
    }

    @DisplayName("Record the event when a court case is received and messageId is null")
    @Test
    void whenCourtCaseReceived_andMessageIdIsNull_thenRecord() {

        telemetryService.trackCourtCaseEvent(courtCase, null);

        verify(telemetryClient).trackEvent(eq("PiCCourtCaseReceived"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        assertCourtCaseProperties();
    }

    @DisplayName("Record the event when a court case is received as JSON and messageId is not null")
    @Test
    void whenCourtCaseReceivedFromJson_andMessageIdIsNull_thenRecord() {

        var sessionStartTime = LocalDateTime.of(DATE_OF_HEARING, LocalTime.of(9, 30, 34));
        var caseJson = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(COURT_CODE)
                        .courtRoom(COURT_ROOM)
                        .sessionStartTime(sessionStartTime)
                        .build()))
                .caseNo(CASE_NO)
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

    @DisplayName("Record the event when the received court case has changed")
    @Test
    void whenUpdatedCaseReceived_thenRecord() {

        telemetryService.trackHearingChangedEvent(courtCase);

        verify(telemetryClient).trackEvent(eq("PiCHearingChanged"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        assertCourtCaseProperties();
    }

    @DisplayName("Record the event when the received court case has not changed")
    @Test
    void whenCaseReceivedHasNoChange_thenRecord() {

        telemetryService.trackHearingUnChangedEvent(courtCase);

        verify(telemetryClient).trackEvent(eq("PiCHearingUnchanged"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        assertCourtCaseProperties();
    }

    private void assertCourtCaseProperties(int size) {
        Map<String, String> properties = propertiesCaptor.getValue();
        assertThat(properties).hasSize(size);
        assertThat(properties).contains(
          entry(COURT_CODE_KEY, courtCase.getCourtCode()),
          entry(URN_KEY, courtCase.getUrn()),
          entry(HEARING_ID_KEY, courtCase.getHearingId()),
          entry(HEARING_DATE_KEY, courtCase.getDateOfHearing().toString()),
          entry(SOURCE_KEY, courtCase.getSource().name()),
          entry(COURT_ROOM_KEY, courtCase.getCourtRoom()),
          entry(CASE_NO_KEY, courtCase.getCaseNo()),
          entry(CASE_ID_KEY, courtCase.getCaseId()),
          entry(DEFENDANT_IDS_KEY, String.join(",", DEFENDANT_ID_ONE, DEFENDANT_ID_TWO))
        );
    }

    private void assertCourtCaseProperties() {
        assertCourtCaseProperties(9);
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
