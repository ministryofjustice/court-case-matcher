package uk.gov.justice.probation.courtcasematcher.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;
import uk.gov.justice.probation.courtcasematcher.model.domain.ProbationStatusDetail;
import uk.gov.justice.probation.courtcasematcher.repository.CourtCaseRepository;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.SearchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.SearchResponses;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.SearchResult;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtCaseServiceTest {

    private static final String DEFENDANT_UUID_1 = "858e8102-5005-4702-a7c5-5f6baa966d59";
    private static final String DEFENDANT_UUID_2 = "ea66c23c-9c9c-4623-8c47-b882007915c3";
    private static final String COURT_CODE = "B10JQ00";
    private static final String COURT_ROOM = "1";
    private static final String CASE_NO = "1234567890";
    private static final String CRN = "X340741";
    private static final String CASE_ID = "c468042b-5ecd-4ce9-a77e-20ad07616e2c";
    private static final Defendant DEFENDANT = Defendant.builder().defendantId(DEFENDANT_UUID_1).build();

    @Captor
    private ArgumentCaptor<CourtCase> courtCaseCaptor;

    @Mock
    private CourtCaseRepository courtCaseRepo;

    @Mock
    private OffenderSearchRestClient offenderSearchRestClient;

    @InjectMocks
    private CourtCaseService courtCaseService;
    private final GroupedOffenderMatches matches = GroupedOffenderMatches.builder()
            .matches(Collections.emptyList())
            .build();

    @DisplayName("Save court case")
    @Test
    void whenSaveCourtCase() {
        final var courtCase = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(COURT_CODE)
                        .build()))
                .caseNo(CASE_NO)
                .caseId(CASE_ID)
                .defendants(List.of(DEFENDANT))
                .groupedOffenderMatches(matches)
                .source(DataSource.LIBRA)
                .build();
        when(courtCaseRepo.putCourtCase(courtCase)).thenReturn(Mono.empty());
        when(courtCaseRepo.postOffenderMatches(CASE_ID, DEFENDANT_UUID_1, matches)).thenReturn(Mono.empty());

        courtCaseService.saveCourtCase(courtCase);

        verify(courtCaseRepo).putCourtCase(courtCase);
        verify(courtCaseRepo).postOffenderMatches(CASE_ID, DEFENDANT_UUID_1, matches);
    }

    @DisplayName("Save court case with no caseNo or caseId.")
    @Test
    void givenNoCaseNoOrId_whenSaveCourtCaseWithCaseId() {
        final var courtCase = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(COURT_CODE)
                        .build()))
                .defendants(List.of(DEFENDANT))
                .groupedOffenderMatches(matches)
                .build();
        when(courtCaseRepo.putCourtCase(courtCaseCaptor.capture())).thenReturn(Mono.empty());
        when(courtCaseRepo.postOffenderMatches(notNull(), eq(DEFENDANT_UUID_1), eq(matches))).thenReturn(Mono.empty());

        courtCaseService.saveCourtCase(courtCase);

        verify(courtCaseRepo).putCourtCase(notNull());

        final var capturedCase = courtCaseCaptor.getValue();
        assertThat(capturedCase.getCaseId()).hasSameSizeAs(CASE_ID);
        assertThat(capturedCase.getCaseNo()).isEqualTo(capturedCase.getCaseId());

        verify(courtCaseRepo).postOffenderMatches(notNull(), eq(DEFENDANT_UUID_1), eq(matches));
    }

    @DisplayName("Incoming gateway case which is merged with the existing.")
    @Test
    void givenExistingCase_whenGetCourtCase_thenMergeAndReturn() {
        final var aCase = buildCaseNoMatches();

        final var courtCase = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(COURT_CODE)
                        .courtRoom("2")
                        .build()))
                .defendants(Collections.singletonList(DEFENDANT))
                .caseId(CASE_ID)
                .caseNo(CASE_NO)
                .build();

        when(courtCaseRepo.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.just(courtCase));

        final var updatedCourtCase = courtCaseService.getCourtCase(aCase).block();

        assertThat(updatedCourtCase.getCourtRoom()).isEqualTo(COURT_ROOM);
        verify(courtCaseRepo).getCourtCase(COURT_CODE, CASE_NO);
    }

    @DisplayName("Get court case which is new, return a transformed copy.")
    @Test
    void givenNewCase_whenGetCourtCase_thenReturn() {
        var aCase = buildCaseNoMatches();

        when(courtCaseRepo.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.empty());

        final var newCourtCase = courtCaseService.getCourtCase(aCase).block();

        assertThat(newCourtCase.getCourtCode()).isSameAs(COURT_CODE);
        assertThat(newCourtCase.getCaseNo()).isSameAs(CASE_NO);
        verify(courtCaseRepo).getCourtCase(COURT_CODE, CASE_NO);
    }

    @DisplayName("Save a court case with a search response.")
    @Test
    void givenSearchResponse_whenCreateCourtCase_thenPutCase() {

        final var matchResponse = MatchResponse.builder().build();
        final var courtCase = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(COURT_CODE)
                        .build()))
                .defendants(Collections.singletonList(DEFENDANT))
                .caseId(CASE_ID)
                .caseNo(CASE_NO)
                .groupedOffenderMatches(matches)
                .build();
        when(courtCaseRepo.putCourtCase(courtCase)).thenReturn(Mono.empty());
        when(courtCaseRepo.postOffenderMatches(CASE_ID, DEFENDANT_UUID_1, matches)).thenReturn(Mono.empty());

        courtCaseService.createCase(courtCase, SearchResult.builder().matchResponse(matchResponse).build());

        verify(courtCaseRepo).putCourtCase(courtCase);
        verify(courtCaseRepo).postOffenderMatches(CASE_ID, DEFENDANT_UUID_1, matches);
    }

    @DisplayName("Save a search responses even if case put fails.")
    @Test
    void givenSearchResponse_whenCreateCourtCaseFails_thenPostMatches() {
        final var matchResponse = MatchResponse.builder().build();
        final var courtCase = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(COURT_CODE)
                        .build()))
                .defendants(Collections.singletonList(DEFENDANT))
                .caseId(CASE_ID)
                .caseNo(CASE_NO)
                .groupedOffenderMatches(matches)
                .build();
        when(courtCaseRepo.putCourtCase(courtCase)).thenThrow(new RuntimeException("bang!"));
        when(courtCaseRepo.postOffenderMatches(CASE_ID, DEFENDANT_UUID_1, matches)).thenReturn(Mono.empty());

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> courtCaseService.createCase(courtCase, SearchResult.builder().matchResponse(matchResponse).build()))
                .withMessage("bang!");

        verify(courtCaseRepo).putCourtCase(courtCase);
        verify(courtCaseRepo).postOffenderMatches(CASE_ID, DEFENDANT_UUID_1, matches);
    }

    @DisplayName("Save a court case without a search response.")
    @Test
    void givenNoSearchResponse_whenCreateCourtCase_thenReturn() {
        final var courtCase = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(COURT_CODE)
                        .build()))
                .defendants(Collections.singletonList(DEFENDANT))
                .caseId(CASE_ID)
                .caseNo(CASE_NO)
                .build();
        when(courtCaseRepo.putCourtCase(courtCase)).thenReturn(Mono.empty());
        when(courtCaseRepo.postOffenderMatches(CASE_ID, DEFENDANT_UUID_1, null)).thenReturn(Mono.empty());

        courtCaseService.createCase(courtCase, null);

        verify(courtCaseRepo).putCourtCase(courtCase);
        verify(courtCaseRepo).postOffenderMatches(CASE_ID, DEFENDANT_UUID_1, null);
    }

    @DisplayName("Fetch and update probation status")
    @Test
    void whenUpdateProbationStatus_thenMergeAndReturn() {

        var localDate = LocalDate.of(2020, Month.AUGUST, 20);
        var courtCase = CourtCase.builder()
                .caseNo(CASE_NO)
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(COURT_CODE)
                        .build()))
                .defendants(Collections.singletonList(Defendant.builder()
                        .crn(CRN)
                        .defendantId(DEFENDANT_UUID_1)
                        .probationStatus("PREVIOUSLY_KNOWN")
                        .build()))
                .build();
        var probationStatusDetail = ProbationStatusDetail.builder()
                .status("CURRENT")
                .preSentenceActivity(true)
                .previouslyKnownTerminationDate(localDate)
                .build();
        var searchResponse = SearchResponse.builder().probationStatusDetail(probationStatusDetail).build();

        when(offenderSearchRestClient.search(CRN)).thenReturn(Mono.just(new SearchResponses(List.of(searchResponse))));

        var courtCaseResult = courtCaseService.updateProbationStatusDetail(courtCase).block();

        assertThat(courtCaseResult.getFirstDefendant().getProbationStatus()).isEqualTo("CURRENT");
        assertThat(courtCaseResult.getFirstDefendant().getPreviouslyKnownTerminationDate()).isEqualTo(localDate);
        assertThat(courtCaseResult.getFirstDefendant().getBreach()).isNull();
        assertThat(courtCaseResult.getFirstDefendant().getPreSentenceActivity()).isTrue();
        verify(offenderSearchRestClient).search(CRN);
    }

    @DisplayName("Given more than one SearchResponse from offender search, ignore and keep probation status as-is")
    @Test
    void givenMultipleSearchResponses_whenUpdateProbationStatus_thenIgnore() {

        var localDate = LocalDate.of(2020, Month.AUGUST, 20);
        var courtCase = CourtCase.builder()
                .caseNo(CASE_NO)
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(COURT_CODE)
                        .build()))
                .defendants(Collections.singletonList(Defendant.builder()
                        .crn(CRN)
                        .probationStatus("PREVIOUSLY_KNOWN")
                        .build()))
                .build();
        var probationStatusDetail = ProbationStatusDetail.builder()
                .status("CURRENT")
                .preSentenceActivity(true)
                .previouslyKnownTerminationDate(localDate)
                .inBreach(Boolean.TRUE)
                .build();
        var searchResponse1 = SearchResponse.builder().probationStatusDetail(probationStatusDetail).build();
        var searchResponse2 = SearchResponse.builder().probationStatusDetail(probationStatusDetail).build();

        when(offenderSearchRestClient.search(CRN)).thenReturn(Mono.just(new SearchResponses(List.of(searchResponse1, searchResponse2))));

        var courtCaseResult = courtCaseService.updateProbationStatusDetail(courtCase).block();

        assertThat(courtCaseResult.getFirstDefendant().getProbationStatus()).isEqualTo("PREVIOUSLY_KNOWN");
        assertThat(courtCaseResult.getFirstDefendant().getPreviouslyKnownTerminationDate()).isNull();
        assertThat(courtCaseResult.getFirstDefendant().getBreach()).isNull();
        assertThat(courtCaseResult.getFirstDefendant().getPreSentenceActivity()).isNull();
        verify(offenderSearchRestClient).search(CRN);
    }

    @DisplayName("When rest client fails to fetch updated probation status then return the original")
    @Test
    void givenFailedCallToRestClient_whenUpdateProbationStatus_thenReturnInput() {

        final var courtCase = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .crn(CRN)
                        .build()))
                .build();
        when(offenderSearchRestClient.search(CRN)).thenReturn(Mono.empty());

        CourtCase courtCaseResult = courtCaseService.updateProbationStatusDetail(courtCase).block();

        assertThat(courtCaseResult).isSameAs(courtCase);
        verify(offenderSearchRestClient).search(CRN);
    }

    private CourtCase buildCaseNoMatches() {

        final var defendant2 = Defendant.builder().defendantId(DEFENDANT_UUID_2).build();
        return CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(COURT_CODE)
                        .courtRoom(COURT_ROOM)
                        .build()))
                .defendants(List.of(DEFENDANT, defendant2))
                .caseNo(CASE_NO)
                .caseId(CASE_ID)
                .source(DataSource.COMMON_PLATFORM)
                .build();
    }


}
