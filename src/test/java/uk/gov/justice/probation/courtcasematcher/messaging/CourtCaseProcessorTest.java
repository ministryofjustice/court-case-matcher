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
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OffenderSearchMatchType;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.SearchResult;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
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
        var matchResponse = MatchResponse.builder().build();
        var searchResult = SearchResult.builder().matchResponse(matchResponse).build();
        var courtCase = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("SHF")
                        .build()))
                .defendants(Collections.singletonList(Defendant.builder()
                        .type(PERSON)
                        .build()))
                .build();
        when(courtCaseService.getCourtCase(any(CourtCase.class))).thenReturn(Mono.just(courtCase));
        when(matcherService.getSearchResponse(any(CourtCase.class))).thenReturn(Mono.just(searchResult));

        messageProcessor.process(courtCase, MESSAGE_ID);

        verify(telemetryService).trackCourtCaseEvent(any(CourtCase.class), eq(MESSAGE_ID));
        verify(telemetryService).trackOffenderMatchEvent(eq(courtCase), eq(matchResponse));
        verify(courtCaseService).createCase(eq(courtCase), eq(searchResult));
        verify(courtCaseService).getCourtCase(any(CourtCase.class));
        verifyNoMoreInteractions(courtCaseService, telemetryService);
    }

    @DisplayName("Given an error in matcher, track that failure and save with no matches")
    @Test
    void givenErrorInMatcherService_whenReceiveCase_thenSave() {
        var courtCase = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("SHF")
                        .build()))
                .defendants(Collections.singletonList(Defendant.builder()
                        .type(PERSON)
                        .build()))
                .build();
        var matchResponse = MatchResponse.builder().matchedBy(OffenderSearchMatchType.NOTHING).matches(Collections.emptyList()).build();
        var errorSearchResult = SearchResult.builder().matchResponse(matchResponse).build();
        when(courtCaseService.getCourtCase(any(CourtCase.class))).thenReturn(Mono.just(courtCase));
        when(matcherService.getSearchResponse(courtCase)).thenReturn(Mono.error(new IllegalArgumentException()));

        messageProcessor.process(courtCase, MESSAGE_ID);

        verify(telemetryService).trackCourtCaseEvent(any(CourtCase.class), eq(MESSAGE_ID));
        verify(telemetryService).trackOffenderMatchFailureEvent(eq(courtCase));
        verify(courtCaseService).getCourtCase(any(CourtCase.class));
        verify(courtCaseService, timeout(MATCHER_THREAD_TIMEOUT)).createCase(eq(courtCase), eq(errorSearchResult));
        verifyNoMoreInteractions(courtCaseService, telemetryService);
    }

    @DisplayName("Receive a case which does not need matching, then update detail and save")
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
        when(courtCaseService.getCourtCase(any(CourtCase.class))).thenReturn(Mono.just(courtCase));
        when(courtCaseService.updateProbationStatusDetail(courtCase)).thenReturn(Mono.just(courtCase));

        messageProcessor.process(courtCase, MESSAGE_ID);

        verify(telemetryService).trackCourtCaseEvent(any(CourtCase.class), eq(MESSAGE_ID));
        verify(courtCaseService).getCourtCase(any(CourtCase.class));
        verify(courtCaseService).updateProbationStatusDetail(eq(courtCase));
        verify(courtCaseService, timeout(MATCHER_THREAD_TIMEOUT)).saveCourtCase(eq(courtCase));
        verifyNoMoreInteractions(courtCaseService, telemetryService);
    }

    @Test
    void givenNullCourtCase_thenThrowRuntimeException() {
        assertThrows(RuntimeException.class, () -> messageProcessor.process(null, MESSAGE_ID));
    }
}