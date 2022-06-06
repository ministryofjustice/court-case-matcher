package uk.gov.justice.probation.courtcasematcher.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType.PERSON;

@ExtendWith(MockitoExtension.class)
class CourtCaseProcessorTest {
    private static final long MATCHER_THREAD_TIMEOUT = 4000;
    private static final String MESSAGE_ID = "messageId";

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private CourtCaseService courtCaseService;

    @Mock
    private MatcherService matcherService;

    private CourtCaseProcessor messageProcessor;

    @BeforeEach
    void beforeEach() {
        messageProcessor = new CourtCaseProcessor(telemetryService,
                courtCaseService,
                matcherService
        );

    }

    @DisplayName("Receive a valid unmatched case for person then match and save")
    @Test
    void whenValidMessageReceivedForPerson_ThenMatchAndSave() {
        var courtCase = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("SHF")
                        .build()))
                .defendants(Collections.singletonList(Defendant.builder()
                        .cro("CRO")
                        .type(PERSON)
                        .build()))
                .build();

        when(courtCaseService.findCourtCase(any(CourtCase.class))).thenReturn(Mono.empty());
        when(matcherService.matchDefendants(any(CourtCase.class))).thenReturn(Mono.just(courtCase));

        messageProcessor.process(courtCase, MESSAGE_ID);

        verify(telemetryService).trackCourtCaseEvent(any(CourtCase.class), eq(MESSAGE_ID));

        verify(courtCaseService).saveCourtCase(eq(courtCase));
        verify(courtCaseService).findCourtCase(any(CourtCase.class));
        verifyNoMoreInteractions(courtCaseService, telemetryService);
    }

    @DisplayName("Receive a case which has changed, then update detail and save")
    @Test
    void whenValidMessageReceivedForMatchedCase_ThenUpdateAndSave() {
        var courtCase = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("SHF")
                        .build()))
                .defendants(Collections.singletonList(Defendant.builder()
                        .type(PERSON)
                        .crn("X320741")
                        .build()))
                .build();
        var existingCourtCase = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .type(PERSON)
                        .crn("X320741")
                        .build()))
                .build();
        var courtCaseMerged = CaseMapper.merge(courtCase, existingCourtCase);
        when(courtCaseService.findCourtCase(any(CourtCase.class))).thenReturn(Mono.just(existingCourtCase));
        when(courtCaseService.updateProbationStatusDetail(courtCaseMerged)).thenReturn(Mono.just(courtCaseMerged));

        messageProcessor.process(courtCase, MESSAGE_ID);

        verify(telemetryService).trackHearingChangedEvent(any(CourtCase.class));
        verify(courtCaseService).findCourtCase(any(CourtCase.class));
        verify(courtCaseService).updateProbationStatusDetail(eq(courtCaseMerged));
        verify(courtCaseService, timeout(MATCHER_THREAD_TIMEOUT)).saveCourtCase(eq(courtCaseMerged));
        verifyNoMoreInteractions(courtCaseService, telemetryService, matcherService);
    }

    @DisplayName("Receive a case which has not changed, then just tack un changed event")
    @Test
    void whenValidMessageReceivedWithNoCaseChange_ThenJustTrackEvent() {
        var courtCase = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .type(PERSON)
                        .crn("X320741")
                        .build()))
                .build();
        var existingCourtCase = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .type(PERSON)
                        .crn("X320741")
                        .build()))
                .build();
        when(courtCaseService.findCourtCase(any(CourtCase.class))).thenReturn(Mono.just(existingCourtCase));

        messageProcessor.process(courtCase, MESSAGE_ID);

        verify(telemetryService).trackHearingUnChangedEvent(any(CourtCase.class));
        verify(courtCaseService).findCourtCase(any(CourtCase.class));
        verifyNoMoreInteractions(courtCaseService, telemetryService, matcherService);
    }

    @Test
    void givenNullCourtCase_thenThrowRuntimeException() {
        assertThrows(RuntimeException.class, () -> messageProcessor.process(null, MESSAGE_ID));
    }
}
