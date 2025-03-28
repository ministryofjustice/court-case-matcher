package uk.gov.justice.probation.courtcasematcher.service;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OSOffender;
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
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.AWAITING_PSR_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.CASE_ID_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.CASE_NO_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.COURT_CODE_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.COURT_ROOM_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.CRNS_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.CRN_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.DEFENDANT_IDS_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.DEFENDANT_ID_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.HEARING_DATE_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.HEARING_ID_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.IN_BREACH_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.MATCHED_BY_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.MATCHES_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.PERSON_ID_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.PNC_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.PREVIOUSLY_KNOWN_TERMINATION_DATE_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.PRE_SENTENCE_ACTVITY_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.PROBATION_STATUS_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.SOURCE_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.SQS_MESSAGE_ID_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.URN_KEY;

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
    private static final String PROBATION_STATUS = "CURRENT";
    private static Hearing hearing;

    @Captor
    private ArgumentCaptor<Map<String, String>> propertiesCaptor;

    @Mock
    private TelemetryClient telemetryClient;

    @InjectMocks
    private TelemetryService telemetryService;

    @BeforeAll
    static void beforeEach() {

        hearing = Hearing.builder()
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
    void whenHearingMessageReceived_thenRecord() {
        telemetryService.trackHearingMessageReceivedEvent("messageId");

        verify(telemetryClient).trackEvent(eq("PiCHearingMessageReceived"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();
        assertThat(properties).hasSize(1);
        assertThat(properties).contains(
                entry("sqsMessageId", "messageId")
        );
    }

    @DisplayName("Record the event when an sqs message event happens and the messageId is null")
    @Test
    void whenHearingMessageReceivedAndMessageIdNull_thenRecord() {
        telemetryService.trackHearingMessageReceivedEvent(null);

        verify(telemetryClient).trackEvent(eq("PiCHearingMessageReceived"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

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

        telemetryService.trackOffenderMatchEvent(DEFENDANT, hearing, response);

        verify(telemetryClient).trackEvent(eq("PiCOffenderExactMatch"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();
        assertHearingProperties(14);
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

        telemetryService.trackOffenderMatchEvent(DEFENDANT, hearing, response);

        verify(telemetryClient).trackEvent(eq("PiCOffenderPartialMatch"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        assertHearingProperties(14);
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

        telemetryService.trackOffenderMatchEvent(DEFENDANT, hearing, response);

        verify(telemetryClient).trackEvent(eq("PiCOffenderPartialMatch"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();
        assertHearingProperties(14);
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

        telemetryService.trackOffenderMatchEvent(DEFENDANT, hearing, response);

        verify(telemetryClient).trackEvent(eq("PiCOffenderNoMatch"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        assertHearingProperties(10);
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
        telemetryService.trackOffenderMatchFailureEvent(defendant, hearing);

        verify(telemetryClient).trackEvent(eq("PiCOffenderMatchError"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        assertHearingProperties(11);
        assertThat(propertiesCaptor.getValue()).contains(
                entry(PNC_KEY, PNC)
        );
    }

    @DisplayName("Record the event when a hearing is received")
    @Test
    void whenHearingReceived_thenRecord() {

        telemetryService.trackNewHearingEvent(hearing, "messageId");

        verify(telemetryClient).trackEvent(eq("PiCHearingReceived"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();

        assertHearingProperties(10);
        assertThat(properties).contains(
                entry(SQS_MESSAGE_ID_KEY, "messageId")
        );
    }

    @DisplayName("Record the event when a hearing is received and messageId is null")
    @Test
    void whenHearingReceived_andMessageIdIsNull_thenRecord() {

        telemetryService.trackNewHearingEvent(hearing, null);

        verify(telemetryClient).trackEvent(eq("PiCHearingReceived"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        assertHearingProperties();
    }

    @DisplayName("Record the event when a hearing is received as JSON and messageId is not null")
    @Test
    void whenHearingReceivedFromJson_andMessageIdIsNull_thenRecord() {

        var sessionStartTime = LocalDateTime.of(DATE_OF_HEARING, LocalTime.of(9, 30, 34));
        var hearingJson = Hearing.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(COURT_CODE)
                        .courtRoom(COURT_ROOM)
                        .sessionStartTime(sessionStartTime)
                        .build()))
                .caseNo(CASE_NO)
                .build();

        telemetryService.trackNewHearingEvent(hearingJson, "messageId");

        verify(telemetryClient).trackEvent(eq("PiCHearingReceived"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

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

    @DisplayName("Record the event when the received hearing has changed")
    @Test
    void whenUpdatedHearingReceived_thenRecord() {

        telemetryService.trackHearingChangedEvent(hearing);

        verify(telemetryClient).trackEvent(eq("PiCHearingChanged"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        assertHearingProperties();
    }

    @DisplayName("Record the event when the received hearing has not changed")
    @Test
    void whenHearingReceivedHasNoChange_thenRecord() {

        telemetryService.trackHearingUnChangedEvent(hearing);

        verify(telemetryClient).trackEvent(eq("PiCHearingUnchanged"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        assertHearingProperties();
    }

    @Test
    void whenProcessingFails_thenRecord() {
        telemetryService.trackProcessingFailureEvent(hearing);

        verify(telemetryClient).trackEvent(eq("PiCMatcherProcessingFailure"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        assertHearingProperties();
    }

    @Test
    void whenDefendantProbationStatusUpdated_thenRecord() {
        final var defendant = Defendant.builder()
                .defendantId(DEFENDANT_ID_ONE)
                .crn(CRN)
                .awaitingPsr(true)
                .breach(true)
                .preSentenceActivity(true)
                .previouslyKnownTerminationDate(DATE_OF_HEARING)
                .probationStatus(PROBATION_STATUS)
                .build();
        telemetryService.trackDefendantProbationStatusUpdatedEvent(defendant);

        verify(telemetryClient).trackEvent(eq("PiCDefendantProbationStatusUpdated"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        final var properties = propertiesCaptor.getValue();
        assertDefendantProperties(properties);
    }

    @Test
    void whenDefendantProbationStatusNotUpdated_thenRecord() {
        final var defendant = Defendant.builder()
                .defendantId(DEFENDANT_ID_ONE)
                .crn(CRN)
                .awaitingPsr(true)
                .breach(true)
                .preSentenceActivity(true)
                .previouslyKnownTerminationDate(DATE_OF_HEARING)
                .probationStatus(PROBATION_STATUS)
                .build();

        telemetryService.trackDefendantProbationStatusNotUpdatedEvent(defendant);

        verify(telemetryClient).trackEvent(eq("PiCDefendantProbationStatusNotUpdated"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        final var properties = propertiesCaptor.getValue();
        assertDefendantProperties(properties);
    }

    @Test
    void shouldRecordPersonCreatedRecordEvent() {
        // Given
        final var defendant = Defendant.builder()
                .defendantId(DEFENDANT_ID_ONE)
                .crn(CRN)
                .pnc(PNC)
                .personId("098098sf098sf09s8f")
                .build();

        // When
        telemetryService.trackPersonRecordCreatedEvent(defendant, hearing);

        // Then
        verify(telemetryClient).trackEvent(eq("PiCPersonRecordCreated"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        var properties = propertiesCaptor.getValue();
        assertThat(properties).hasSize(5)
            .contains(
                entry(HEARING_ID_KEY, hearing.getHearingId()),
                entry(PNC_KEY, defendant.getPnc()),
                entry(PERSON_ID_KEY, defendant.getPersonId()),
                entry(DEFENDANT_ID_KEY, defendant.getDefendantId()),
                entry(CASE_ID_KEY, hearing.getCaseId()));
    }

    private void assertDefendantProperties(Map<String, String> properties) {
        assertThat(properties).hasSize(7);
        assertThat(properties).contains(
                entry(DEFENDANT_ID_KEY, DEFENDANT_ID_ONE),
                entry(CRN_KEY, CRN),
                entry(AWAITING_PSR_KEY, "true"),
                entry(IN_BREACH_KEY, "true"),
                entry(PRE_SENTENCE_ACTVITY_KEY, "true"),
                entry(PREVIOUSLY_KNOWN_TERMINATION_DATE_KEY, DATE_OF_HEARING.toString()),
                entry(PROBATION_STATUS_KEY, PROBATION_STATUS)
        );
    }

    private void assertHearingProperties(int size) {
        Map<String, String> properties = propertiesCaptor.getValue();
        assertThat(properties).hasSize(size);
        assertThat(properties).contains(
          entry(COURT_CODE_KEY, hearing.getCourtCode()),
          entry(URN_KEY, hearing.getUrn()),
          entry(HEARING_ID_KEY, hearing.getHearingId()),
          entry(HEARING_DATE_KEY, hearing.getDateOfHearing().toString()),
          entry(SOURCE_KEY, hearing.getSource().name()),
          entry(COURT_ROOM_KEY, hearing.getCourtRoom()),
          entry(CASE_NO_KEY, hearing.getCaseNo()),
          entry(CASE_ID_KEY, hearing.getCaseId()),
          entry(DEFENDANT_IDS_KEY, String.join(",", DEFENDANT_ID_ONE, DEFENDANT_ID_TWO))
        );
    }

    private void assertHearingProperties() {
        assertHearingProperties(9);
    }

    private Match buildMatch(String crn) {
        return Match.builder()
                .offender(OSOffender.builder()
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
