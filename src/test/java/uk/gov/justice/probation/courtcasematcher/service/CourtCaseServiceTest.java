package uk.gov.justice.probation.courtcasematcher.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtCaseServiceTest {

    private static final String COURT_CODE = "B10JQ00";
    private static final String COURT_ROOM = "1";
    private static final String CASE_NO = "1234567890";
    private static final String CRN = "X340741";
    private static final Long CASE_ID = 321344L;

    @Mock
    private CourtCaseRepository courtCaseRepo;

    @Mock
    private OffenderSearchRestClient offenderSearchRestClient;

    @InjectMocks
    private CourtCaseService courtCaseService;
    private final GroupedOffenderMatches matches = GroupedOffenderMatches.builder()
            .matches(Collections.emptyList())
            .build();

    @DisplayName("Save court case.")
    @Test
    void saveCourtCase() {
        final var courtCase = CourtCase.builder()
                .caseNo(CASE_NO)
                .courtCode(COURT_CODE)
                .groupedOffenderMatches(matches)
                .build();
        when(courtCaseRepo.putCourtCase(courtCase)).thenReturn(Mono.empty());
        when(courtCaseRepo.postMatches(COURT_CODE, CASE_NO, matches)).thenReturn(Mono.empty());

        courtCaseService.saveCourtCase(courtCase);

        verify(courtCaseRepo).putCourtCase(courtCase);
        verify(courtCaseRepo).postMatches(COURT_CODE, CASE_NO, matches);
    }

    @DisplayName("Incoming gateway case which is merged with the existing.")
    @Test
    void givenExistingCase_whenGetCourtCase_thenMergeAndReturn() {
        var aCase = buildCase();

        CourtCase courtCase = CourtCase.builder()
                .caseId(Long.toString(CASE_ID))
                .caseNo(CASE_NO)
                .courtCode(COURT_CODE)
                .courtRoom("2")
                .build();

        when(courtCaseRepo.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.just(courtCase));

        CourtCase updatedCourtCase = courtCaseService.getCourtCase(aCase).block();

        assertThat(updatedCourtCase.getCourtRoom()).isEqualTo(COURT_ROOM);
        verify(courtCaseRepo).getCourtCase(COURT_CODE, CASE_NO);
    }

    @DisplayName("Get court case which is new, return a transformed copy.")
    @Test
    void givenNewCase_whenGetCourtCase_thenReturn() {
        var aCase = buildCase();

        when(courtCaseRepo.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.empty());

        CourtCase newCourtCase = courtCaseService.getCourtCase(aCase).block();

        assertThat(newCourtCase.getCourtCode()).isSameAs(COURT_CODE);
        assertThat(newCourtCase.getCaseNo()).isSameAs(CASE_NO);
        verify(courtCaseRepo).getCourtCase(COURT_CODE, CASE_NO);
    }

    @DisplayName("Save a court case with a search response.")
    @Test
    void givenSearchResponse_whenCreateCourtCase_thenPutCase() {
        MatchResponse matchResponse = MatchResponse.builder().build();
        final var courtCase = CourtCase.builder()
                            .caseId(Long.toString(CASE_ID))
                            .caseNo(CASE_NO)
                            .courtCode(COURT_CODE)
                            .groupedOffenderMatches(matches)
                            .build();
        when(courtCaseRepo.putCourtCase(courtCase)).thenReturn(Mono.empty());
        when(courtCaseRepo.postMatches(COURT_CODE, CASE_NO, matches)).thenReturn(Mono.empty());

        courtCaseService.createCase(courtCase, SearchResult.builder().matchResponse(matchResponse).build());

        verify(courtCaseRepo).putCourtCase(courtCase);
        verify(courtCaseRepo).postMatches(COURT_CODE, CASE_NO, matches);
    }

    @DisplayName("Save a search responses even if case put fails.")
    @Test
    void givenSearchResponse_whenCreateCourtCaseFails_thenPostMatches() {
        MatchResponse matchResponse = MatchResponse.builder().build();
        final var courtCase = CourtCase.builder()
                            .caseId(Long.toString(CASE_ID))
                            .caseNo(CASE_NO)
                            .courtCode(COURT_CODE)
                            .groupedOffenderMatches(matches)
                            .build();
        when(courtCaseRepo.putCourtCase(courtCase)).thenThrow(new RuntimeException("bang!"));
        when(courtCaseRepo.postMatches(COURT_CODE, CASE_NO, matches)).thenReturn(Mono.empty());

        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> courtCaseService.createCase(courtCase, SearchResult.builder().matchResponse(matchResponse).build()))
            .withMessage("bang!");

        verify(courtCaseRepo).putCourtCase(courtCase);
        verify(courtCaseRepo).postMatches(COURT_CODE, CASE_NO, matches);
    }

    @DisplayName("Save a court case without a search response.")
    @Test
    void givenNoSearchResponse_whenCreateCourtCase_thenReturn() {
        final var courtCase = CourtCase.builder().caseId(Long.toString(CASE_ID)).caseNo(CASE_NO).courtCode(COURT_CODE).build();
        when(courtCaseRepo.putCourtCase(courtCase)).thenReturn(Mono.empty());
        when(courtCaseRepo.postMatches(COURT_CODE, CASE_NO, null)).thenReturn(Mono.empty());

        courtCaseService.createCase(courtCase, null);

        verify(courtCaseRepo).putCourtCase(courtCase);
        verify(courtCaseRepo).postMatches(COURT_CODE, CASE_NO, null);
    }

    @DisplayName("Fetch and update probation status")
    @Test
    void whenUpdateProbationStatus_thenMergeAndReturn() {

        var localDate = LocalDate.of(2020, Month.AUGUST, 20);
        var courtCase = CourtCase.builder().crn(CRN).courtCode(COURT_CODE).caseNo(CASE_NO).probationStatus("Previously known")
            .probationStatusActual("PREVIOUSLY_KNOWN").build();
        var probationStatusDetail = ProbationStatusDetail.builder()
                                                                    .status("CURRENT")
                                                                    .preSentenceActivity(true)
                                                                    .previouslyKnownTerminationDate(localDate)
                                                                    .build();
        var searchResponse = SearchResponse.builder().probationStatusDetail(probationStatusDetail).build();

        when(offenderSearchRestClient.search(CRN)).thenReturn(Mono.just(new SearchResponses(List.of(searchResponse))));

        var courtCaseResult = courtCaseService.updateProbationStatusDetail(courtCase).block();

        assertThat(courtCaseResult.getProbationStatus()).isEqualTo("CURRENT");
        assertThat(courtCaseResult.getPreviouslyKnownTerminationDate()).isEqualTo(localDate);
        assertThat(courtCaseResult.getBreach()).isNull();
        assertThat(courtCaseResult.isPreSentenceActivity()).isTrue();
        verify(offenderSearchRestClient).search(CRN);
    }

    @DisplayName("Given more than one SearchResponse from offender search, ignore and keep probation status as-is")
    @Test
    void givenMultipleSearchResponses_whenUpdateProbationStatus_thenIgnore() {

        var localDate = LocalDate.of(2020, Month.AUGUST, 20);
        var courtCase = CourtCase.builder().crn(CRN).courtCode(COURT_CODE).caseNo(CASE_NO)
            .probationStatus("Previously known")
            .probationStatusActual("PREVIOUSLY_KNOWN")
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

        assertThat(courtCaseResult.getProbationStatus()).isEqualTo("Previously known");
        assertThat(courtCaseResult.getProbationStatusActual()).isEqualTo("PREVIOUSLY_KNOWN");
        assertThat(courtCaseResult.getPreviouslyKnownTerminationDate()).isNull();
        assertThat(courtCaseResult.getBreach()).isNull();
        assertThat(courtCaseResult.isPreSentenceActivity()).isFalse();
        verify(offenderSearchRestClient).search(CRN);
    }

    @DisplayName("When rest client fails to fetch updated probation status then return the original")
    @Test
    void givenFailedCallToRestClient_whenUpdateProbationStatus_thenReturnInput() {

        CourtCase courtCase = CourtCase.builder().crn(CRN).build();
        when(offenderSearchRestClient.search(CRN)).thenReturn(Mono.empty());

        CourtCase courtCaseResult = courtCaseService.updateProbationStatusDetail(courtCase).block();

        assertThat(courtCaseResult).isSameAs(courtCase);
        verify(offenderSearchRestClient).search(CRN);
    }

    private CourtCase buildCase() {
        return CourtCase.builder()
            .courtCode(COURT_CODE)
            .courtRoom(COURT_ROOM)
            .caseNo(CASE_NO)
            .caseId(CASE_ID.toString())
            .build();
    }
}
