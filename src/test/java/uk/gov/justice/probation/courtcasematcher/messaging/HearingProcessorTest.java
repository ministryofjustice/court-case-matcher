package uk.gov.justice.probation.courtcasematcher.messaging;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;
import uk.gov.justice.probation.courtcasematcher.model.mapper.HearingMapper;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.justice.probation.courtcasematcher.model.type.DefendantType.PERSON;

@ExtendWith(MockitoExtension.class)
class HearingProcessorTest {
    private static final long MATCHER_THREAD_TIMEOUT = 4000;
    private static final String MESSAGE_ID = "messageId";

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private CourtCaseService courtCaseService;

    @Mock
    private MatcherService matcherService;

    private HearingProcessor messageProcessor;

    @Captor
    private ArgumentCaptor<Hearing> hearingArgumentCaptor;

    @BeforeEach
    void beforeEach() {
        messageProcessor = new HearingProcessor(telemetryService,
                courtCaseService,
                matcherService
        );

    }

    @DisplayName("Receive a valid unmatched case for person then match and save")
    @Test
    void whenValidMessageReceivedForPerson_ThenMatchAndSave() {
        var courtCase = Hearing.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("SHF")
                        .build()))
                .defendants(Collections.singletonList(Defendant.builder()
                        .cro("CRO")
                        .type(PERSON)
                        .build()))
                .build();

        when(courtCaseService.findHearing(any(Hearing.class))).thenReturn(Mono.empty());
        when(matcherService.matchDefendants(any(Hearing.class))).thenReturn(Mono.just(courtCase));

        messageProcessor.process(courtCase, MESSAGE_ID);

        verify(telemetryService).trackHearingEvent(any(Hearing.class), eq(MESSAGE_ID));

        verify(courtCaseService).saveHearing(eq(courtCase));
        verify(courtCaseService).findHearing(any(Hearing.class));
        verifyNoMoreInteractions(courtCaseService, telemetryService);
    }

    @DisplayName("Receive a case which has changed, then update detail and save")
    @Test
    void whenValidMessageReceivedForMatchedCase_ThenUpdateAndSave() {
        var caseId = UUID.randomUUID().toString();
        var defendantId = UUID.randomUUID().toString();

        var courtCase = Hearing.builder()
                .caseId(caseId)
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("SHF")
                        .build()))
                .defendants(Collections.singletonList(Defendant.builder()
                        .type(PERSON)
                        .crn("X320741")
                        .defendantId(defendantId)
                        .build()))
                .build();
        var existingCourtCase = Hearing.builder()
                .caseId(caseId)
                .defendants(Collections.singletonList(Defendant.builder()
                        .type(PERSON)
                        .crn("X320741")
                        .defendantId(defendantId)
                        .build()))
                .build();
        var courtCaseMerged = HearingMapper.merge(courtCase, existingCourtCase);
        when(courtCaseService.findHearing(any(Hearing.class))).thenReturn(Mono.just(existingCourtCase));
        when(courtCaseService.updateProbationStatusDetail(courtCaseMerged)).thenReturn(Mono.just(courtCaseMerged));

        messageProcessor.process(courtCase, MESSAGE_ID);

        verify(telemetryService).trackHearingChangedEvent(any(Hearing.class));
        verify(courtCaseService).findHearing(any(Hearing.class));
        verify(courtCaseService).updateProbationStatusDetail(eq(courtCaseMerged));
        verify(courtCaseService, timeout(MATCHER_THREAD_TIMEOUT)).saveHearing(eq(courtCaseMerged));
        verifyNoMoreInteractions(courtCaseService, telemetryService, matcherService);
    }

    @DisplayName("Update hearing ID when listNo updated for Libra cases")
    @Test
    void when_new_listNo_receivedForExistingLibraCase_createNewHearingId_ThenUpdateAndSave() {
        var caseId = UUID.randomUUID().toString();
        var defendantId = UUID.randomUUID().toString();

        var courtCase = Hearing.builder()
                .source(DataSource.LIBRA)
                .caseId(caseId)
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("SHF")
                        .listNo("1st")
                        .build()))
                .defendants(Collections.singletonList(Defendant.builder()
                        .type(PERSON)
                        .crn("X320741")
                        .defendantId(defendantId)
                        .build()))
                .build();
        var existingCourtCase = Hearing.builder()
                .caseId(caseId)
                .hearingId(caseId)
                .source(DataSource.LIBRA)
                .defendants(Collections.singletonList(Defendant.builder()
                        .type(PERSON)
                        .crn("X320741")
                        .defendantId(defendantId)
                        .build()))
               .hearingDays(Collections.singletonList(HearingDay.builder()
                    .courtCode("SHF")
                    .listNo("2nd")
                    .build()))
                .build();
        var courtCaseMerged = HearingMapper.merge(courtCase, existingCourtCase);
        when(courtCaseService.findHearing(any(Hearing.class))).thenReturn(Mono.just(existingCourtCase));
        when(courtCaseService.updateProbationStatusDetail(hearingArgumentCaptor.capture())).thenReturn(Mono.just(courtCaseMerged));

        messageProcessor.process(courtCase, MESSAGE_ID);

        verify(courtCaseService).findHearing(any(Hearing.class));
        verify(courtCaseService).updateProbationStatusDetail(hearingArgumentCaptor.capture());
        Assertions.assertThat(hearingArgumentCaptor.getValue().getHearingId()).isNotEqualTo(caseId);
    }

    @DisplayName("Use existing hearing ID when listNo not updated for Libra cases")
    @Test
    void when_forExistingLibraCase_listNo_not_changed_useExistingHearingId_ThenUpdateAndSave() {
        var caseId = UUID.randomUUID().toString();
        var defendantId = UUID.randomUUID().toString();

        var courtCase = Hearing.builder()
          .source(DataSource.LIBRA)
          .caseId(caseId)
          .hearingDays(Collections.singletonList(HearingDay.builder()
            .courtCode("SHF")
            .listNo("1st")
            .build()))
          .defendants(Collections.singletonList(Defendant.builder()
            .type(PERSON)
            .crn("X320741")
            .defendantId(defendantId)
            .build()))
          .build();
        var existingCourtCase = Hearing.builder()
          .caseId(caseId)
          .hearingId(caseId)
          .source(DataSource.LIBRA)
          .defendants(Collections.singletonList(Defendant.builder()
            .type(PERSON)
            .crn("X320741")
            .defendantId(defendantId)
            .build()))
          .hearingDays(Collections.singletonList(HearingDay.builder()
            .courtCode("ABC")
            .listNo("1st")
            .build()))
          .build();
        var courtCaseMerged = HearingMapper.merge(courtCase, existingCourtCase);
        when(courtCaseService.findHearing(any(Hearing.class))).thenReturn(Mono.just(existingCourtCase));
        when(courtCaseService.updateProbationStatusDetail(hearingArgumentCaptor.capture())).thenReturn(Mono.just(courtCaseMerged));

        messageProcessor.process(courtCase, MESSAGE_ID);

        verify(courtCaseService).findHearing(any(Hearing.class));
        verify(courtCaseService).updateProbationStatusDetail(hearingArgumentCaptor.capture());
        Assertions.assertThat(hearingArgumentCaptor.getValue().getHearingId()).isEqualTo(caseId);
    }

    @DisplayName("Receive a case which has not changed, then just track un changed event")
    @Test
    void whenValidMessageReceivedWithNoCaseChange_ThenJustTrackEvent() {
        var courtCase = Hearing.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .type(PERSON)
                        .crn("X320741")
                        .build()))
                .build();
        var existingCourtCase = Hearing.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .type(PERSON)
                        .crn("X320741")
                        .build()))
                .build();
        when(courtCaseService.findHearing(any(Hearing.class))).thenReturn(Mono.just(existingCourtCase));

        messageProcessor.process(courtCase, MESSAGE_ID);

        verify(telemetryService).trackHearingUnChangedEvent(any(Hearing.class));
        verify(courtCaseService).findHearing(any(Hearing.class));
        verifyNoMoreInteractions(courtCaseService, telemetryService, matcherService);
    }

    @DisplayName("Receive a same payload multiple times then fire changed event first time and only unchanged event")
    @Test
    void whenValidMessageWithSamePayLoadReceivedMultipleTimes_ThenFireChangedEventFirst_ThenUnChangedEventOnSubsequentCall() {
        var caseId = UUID.randomUUID().toString();
        var defendantId = UUID.randomUUID().toString();
        var courtCase = Hearing.builder()
                .caseId(caseId)
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("SHF")
                        .build()))
                .defendants(Collections.singletonList(Defendant.builder()
                        .type(PERSON)
                        .crn("X320741")
                        .defendantId(defendantId)
                        .build()))
                .build();
        var existingCourtCase = Hearing.builder()
                .caseId(caseId)
                .defendants(Collections.singletonList(Defendant.builder()
                        .type(PERSON)
                        .crn("X320741")
                        .defendantId(defendantId)
                        .build()))
                .build();
        var existingCourtCaseAfterUpdated = Hearing.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("SHF")
                        .build()))
                .defendants(Collections.singletonList(Defendant.builder()
                        .type(PERSON)
                        .crn("X320741")
                        .build()))
                .build();
        var courtCaseMerged = HearingMapper.merge(courtCase, existingCourtCase);
        when(courtCaseService.findHearing(any(Hearing.class)))
                .thenReturn(Mono.just(existingCourtCase))
                .thenReturn(Mono.just(existingCourtCaseAfterUpdated));
        when(courtCaseService.updateProbationStatusDetail(courtCaseMerged)).thenReturn(Mono.just(courtCaseMerged));

        // First call
        messageProcessor.process(courtCase, MESSAGE_ID);

        // First time case change detected so changed event fired
        verify(telemetryService).trackHearingChangedEvent(any(Hearing.class));
        verify(courtCaseService).findHearing(any(Hearing.class));
        verify(courtCaseService).updateProbationStatusDetail(eq(courtCaseMerged));
        verify(courtCaseService, timeout(MATCHER_THREAD_TIMEOUT)).saveHearing(eq(courtCaseMerged));
        verifyNoMoreInteractions(courtCaseService, telemetryService, matcherService);

        // Second call with same case(the existing case was updated first time so matching the payload second time)
        messageProcessor.process(courtCase, MESSAGE_ID);

        verify(telemetryService).trackHearingUnChangedEvent(any(Hearing.class));
        verify(courtCaseService, times(2)).findHearing(any(Hearing.class));
        verifyNoMoreInteractions(courtCaseService, telemetryService, matcherService);

        // Third call
        messageProcessor.process(courtCase, MESSAGE_ID);

        verify(telemetryService, times(2)).trackHearingUnChangedEvent(any(Hearing.class));
        verify(courtCaseService, times(3)).findHearing(any(Hearing.class));
        verifyNoMoreInteractions(courtCaseService, telemetryService, matcherService);
    }

    @Test
    void givenNullCourtCase_thenThrowRuntimeException() {
        assertThrows(RuntimeException.class, () -> messageProcessor.process(null, MESSAGE_ID));
    }
}
