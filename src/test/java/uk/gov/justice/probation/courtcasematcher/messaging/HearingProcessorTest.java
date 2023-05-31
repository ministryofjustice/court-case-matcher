package uk.gov.justice.probation.courtcasematcher.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.application.FeatureFlags;
import uk.gov.justice.probation.courtcasematcher.model.domain.CaseMarker;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
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
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
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

    @Mock
    private FeatureFlags featureFlags;

    private HearingProcessor hearingProcessor;

    @BeforeEach
    void beforeEach() {
        hearingProcessor = new HearingProcessor(telemetryService,
                courtCaseService,
                matcherService,
                featureFlags
        );

    }

    @Nested
    class GivenANewCase {

        @BeforeEach
        public void setUp() {
            when(courtCaseService.findHearing(any(Hearing.class))).thenReturn(Mono.empty());
        }

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
                    .caseMarkers(Collections.singletonList(CaseMarker.builder()
                                    .markerTypeDescription("description")
                            .build()))
                    .build();

            when(matcherService.matchDefendants(any(Hearing.class))).thenReturn(Mono.just(courtCase));

            hearingProcessor.process(courtCase, MESSAGE_ID);

            verify(telemetryService).trackNewHearingEvent(any(Hearing.class), eq(MESSAGE_ID));

            verify(matcherService).matchDefendants(any(Hearing.class));
            verify(courtCaseService).saveHearing(eq(courtCase));
            verify(courtCaseService).findHearing(any(Hearing.class));
            verifyNoMoreInteractions(courtCaseService, telemetryService);
        }
        @Nested
        public class AndTheMatchOnEveryNoRecordFlagIsSet {
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

                when(matcherService.matchDefendants(any(Hearing.class))).thenReturn(Mono.just(courtCase));

                hearingProcessor.process(courtCase, MESSAGE_ID);

                verify(telemetryService).trackNewHearingEvent(any(Hearing.class), eq(MESSAGE_ID));

                verify(matcherService).matchDefendants(any(Hearing.class));
                verify(courtCaseService).saveHearing(eq(courtCase));
                verify(courtCaseService).findHearing(any(Hearing.class));
                verifyNoMoreInteractions(courtCaseService, telemetryService);
            }
        }

    }

    @Nested
    class GivenAnExistingCase {

        private String caseId;
        private String defendantId;
        private Hearing existingHearing;
        private Hearing existingNoRecordHearing;
        private Defendant existingNoRecordDefendant;
        private Defendant existingDefendant;

        @BeforeEach
        public void setUp() {
            caseId = UUID.randomUUID().toString();
            defendantId = UUID.randomUUID().toString();
            existingDefendant = Defendant.builder()
                    .type(PERSON)
                    .crn("X320741")
                    .defendantId(defendantId)
                    .build();
            existingHearing = Hearing.builder()
                    .caseId(caseId)
                    .defendants(Collections.singletonList(existingDefendant))
                    .build();
            existingNoRecordDefendant = Defendant.builder()
                    .type(PERSON)
                    .crn(null)
                    .confirmedOffender(false)
                    .defendantId(defendantId)
                    .build();
            existingNoRecordHearing = Hearing.builder()
                    .caseId(caseId)
                    .defendants(Collections.singletonList(existingNoRecordDefendant))
                    .build();

        }

        @Test
        void whenThatCaseHasChanged_ThenUpdateAndSave() {
            when(featureFlags.getFlag("match-on-every-no-record-update")).thenReturn(false);
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
            var courtCaseMerged = HearingMapper.merge(courtCase, existingHearing);
            when(courtCaseService.findHearing(any(Hearing.class))).thenReturn(Mono.just(existingHearing));
            when(courtCaseService.updateProbationStatusDetail(courtCaseMerged)).thenReturn(Mono.just(courtCaseMerged));

            hearingProcessor.process(courtCase, MESSAGE_ID);

            verify(telemetryService).trackHearingChangedEvent(any(Hearing.class));
            verify(courtCaseService).findHearing(any(Hearing.class));
            verify(courtCaseService).updateProbationStatusDetail(eq(courtCaseMerged));
            verify(courtCaseService, timeout(MATCHER_THREAD_TIMEOUT)).saveHearing(eq(courtCaseMerged));
            verifyNoMoreInteractions(courtCaseService, telemetryService, matcherService);
        }
        @Test
        void whenThatCaseHasNotChanged_ThenJustTrackEvent() {
            when(courtCaseService.findHearing(any(Hearing.class))).thenReturn(Mono.just(existingHearing));

            hearingProcessor.process(existingHearing, MESSAGE_ID);

            verify(telemetryService).trackHearingUnChangedEvent(any(Hearing.class));
            verify(courtCaseService).findHearing(any(Hearing.class));
            verifyNoMoreInteractions(courtCaseService, telemetryService, matcherService);
        }
        @Nested
        public class AndTheMatchOnEveryNoRecordFlagIsSet {
            private Hearing hearing;

            @BeforeEach
            public void setUp() {
                when(featureFlags.getFlag("match-on-every-no-record-update")).thenReturn(true);
                hearing = Hearing.builder()
                        .caseId(caseId)
                        .urn("12345")
                        .defendants(Collections.singletonList(existingNoRecordDefendant))
                        .build();
            }


            @Test
            void whenThatCaseHasChanged_andItIsANoRecord_ThenMatchAndSave() {
                var hearingMerged = HearingMapper.merge(hearing, existingNoRecordHearing);
                when(courtCaseService.findHearing(hearing)).thenReturn(Mono.just(existingNoRecordHearing));
                when(matcherService.matchDefendant(existingNoRecordDefendant, hearingMerged)).thenReturn(Mono.just(existingNoRecordDefendant));

                hearingProcessor.process(hearing, MESSAGE_ID);

                verify(matcherService).matchDefendant(eq(existingNoRecordDefendant), eq(hearingMerged));
                verify(telemetryService).trackHearingChangedEvent(any(Hearing.class));
                verify(courtCaseService).findHearing(hearing);
                verify(courtCaseService, timeout(MATCHER_THREAD_TIMEOUT)).saveHearing(eq(hearingMerged));
                verifyNoMoreInteractions(courtCaseService, telemetryService, matcherService);
            }

            @Test
            void whenThatCaseHasChanged_AndItIsNotANoRecord_ThenUpdateAndSave() {
                var courtCaseMerged = HearingMapper.merge(hearing, existingHearing);
                when(courtCaseService.findHearing(hearing)).thenReturn(Mono.just(existingHearing));
                when(courtCaseService.updateDefendant(existingDefendant)).thenReturn(Mono.just(existingDefendant));

                hearingProcessor.process(hearing, MESSAGE_ID);

                verify(courtCaseService).updateDefendant(existingDefendant);
                verify(telemetryService).trackHearingChangedEvent(any(Hearing.class));
                verify(courtCaseService).findHearing(any(Hearing.class));
                verify(courtCaseService, timeout(MATCHER_THREAD_TIMEOUT)).saveHearing(eq(courtCaseMerged));
                verifyNoMoreInteractions(courtCaseService, telemetryService, matcherService);
            }
        }
    }


    @Test
    void givenNullCourtCase_thenThrowRuntimeException() {
        var hearing = Hearing.builder().build();
        assertThrows(RuntimeException.class, () -> hearingProcessor.process(hearing, MESSAGE_ID));
        verify(telemetryService, times(1)).trackProcessingFailureEvent(hearing);
    }
}
