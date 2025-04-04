package uk.gov.justice.probation.courtcasematcher.service;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;
import uk.gov.justice.probation.courtcasematcher.model.domain.ProbationStatusDetail;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseServiceClient;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.SearchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.SearchResponses;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HearingServiceTest {

    private static final String DEFENDANT_UUID_1 = "858e8102-5005-4702-a7c5-5f6baa966d59";
    private static final String DEFENDANT_UUID_2 = "ea66c23c-9c9c-4623-8c47-b882007915c3";
    private static final String COURT_CODE = "B10JQ00";
    private static final String COURT_ROOM = "1";
    private static final String LIST_NO = "2nd";
    private static final String CASE_NO = "1234567890";
    private static final String CRN = "X340741";
    private static final String CASE_ID = "c468042b-5ecd-4ce9-a77e-20ad07616e2c";
    private static final String HEARING_ID = "dd57f899-084a-4241-b264-4d2124c6006f";
    private static final Defendant DEFENDANT = Defendant.builder().defendantId(DEFENDANT_UUID_1).build();
    private static final Defendant DEFENDANT_2 = Defendant.builder().defendantId(DEFENDANT_UUID_2).build();
    private static final List<Defendant> defendants = List.of(DEFENDANT, DEFENDANT_2);
    private static final String CRN_2 = "CRN_2";
    private static final String EXPECTED_PROBATION_STATUS = "CURRENT";

    @Mock
    private TelemetryService telemetryService;
    @Mock
    private OffenderSearchRestClient offenderSearchRestClient;
    @Captor
    private ArgumentCaptor<Hearing> courtCaseCaptor;
    @Mock
    private CourtCaseServiceClient courtCaseServiceClient;
    @InjectMocks
    private CourtCaseService courtCaseService;
    @DisplayName("Save court case. This must be existing because it has a case no and a case id.")
    @Test
    void whenSaveCourtCase() {
        final var courtCase = Hearing.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(COURT_CODE)
                        .build()))
                .caseNo(CASE_NO)
                .caseId(CASE_ID)
                .defendants(defendants)
                .source(DataSource.LIBRA)
                .build();
        when(courtCaseServiceClient.putHearing(courtCase)).thenReturn(Mono.empty());
        when(courtCaseServiceClient.postOffenderMatches(CASE_ID, defendants)).thenReturn(Mono.empty());

        courtCaseService.saveHearing(courtCase);

        verify(courtCaseServiceClient).putHearing(courtCase);
        verify(courtCaseServiceClient).postOffenderMatches(CASE_ID, defendants);
    }

    @DisplayName("Save court case with no caseNo but with a caseId. Indicates a new CP case.")
    @Test
    void givenNoCaseNoOrId_whenSaveCourtCaseWithCaseId() {
        final var courtCase = Hearing.builder()
                .source(DataSource.COMMON_PLATFORM)
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(COURT_CODE)
                        .build()))
                .defendants(defendants)
                .caseId(CASE_ID)
                .build();
        when(courtCaseServiceClient.putHearing(courtCaseCaptor.capture())).thenReturn(Mono.empty());
        when(courtCaseServiceClient.postOffenderMatches(CASE_ID, defendants)).thenReturn(Mono.empty());

        courtCaseService.saveHearing(courtCase);

        verify(courtCaseServiceClient).putHearing(notNull());

        final var capturedCase = courtCaseCaptor.getValue();
        assertThat(capturedCase.getCaseId()).isEqualTo(CASE_ID);
        assertThat(capturedCase.getCaseNo()).isEqualTo(capturedCase.getCaseId());

        verify(courtCaseServiceClient).postOffenderMatches(CASE_ID, defendants);
        verifyNoMoreInteractions(courtCaseServiceClient);
    }

    @DisplayName("Save court case with no caseId but with a caseNo and no defendant ID. Indicates a new LIBRA case.")
    @Test
    void givenNoCaseButHaveCaseNo_whenSaveCourtCaseWithCaseIdRetainCaseNo() {
        var caseId = UUID.randomUUID().toString();
        var defendantId = UUID.randomUUID().toString();
        var defendantIdAnother = UUID.randomUUID().toString();

        final var courtCase = Hearing.builder()
                .caseId(caseId)
                .hearingId("hearingId")
                .source(DataSource.LIBRA)
            .hearingDays(Collections.singletonList(HearingDay.builder()
                .courtCode(COURT_CODE)
                .build()))
            .defendants(List.of(Defendant.builder().defendantId(defendantId).build(), Defendant.builder().defendantId(defendantIdAnother).build()))
            .caseNo(CASE_NO)
            .build();
        when(courtCaseServiceClient.putHearing(courtCaseCaptor.capture())).thenReturn(Mono.empty());
        when(courtCaseServiceClient.postOffenderMatches(notNull(), notNull())).thenReturn(Mono.empty());

        courtCaseService.saveHearing(courtCase);

        verify(courtCaseServiceClient).putHearing(courtCaseCaptor.capture());

        Hearing actual = courtCaseCaptor.getValue();
        assertThat(actual).isNotNull();
        assertThat(actual.getHearingId()).isNotNull();
        assertThat(actual.getCaseId()).isNotNull();
        assertThat(actual.getCaseId()).isEqualTo(caseId);


        final var capturedCase = actual;
        assertThat(capturedCase.getCaseId()).hasSameSizeAs(CASE_ID);
        assertThat(capturedCase.getDefendants().get(0).getDefendantId()).hasSameSizeAs(DEFENDANT_UUID_1);
        assertThat(capturedCase.getDefendants().get(1).getDefendantId()).hasSameSizeAs(DEFENDANT_UUID_1);
        assertThat(capturedCase.getCaseNo()).isEqualTo(CASE_NO);

        verify(courtCaseServiceClient).postOffenderMatches(notNull(), notNull());
        verifyNoMoreInteractions(courtCaseServiceClient);
    }

    @DisplayName("Incoming Libra case returned if exist")
    @Test
    void givenExistingLibraCase_whenGetCourtCase_thenReturn() {
        final var aCase = buildCaseNoMatches()

                .withSource(DataSource.LIBRA);

        final var courtCase = Hearing.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(COURT_CODE)
                        .courtRoom("2")
                        .build()))
                .defendants(defendants)
                .caseId(CASE_ID)
                .caseNo(CASE_NO)
                .source(DataSource.LIBRA)
                .build();

        when(courtCaseServiceClient.getHearing(COURT_CODE, CASE_NO, LIST_NO)).thenReturn(Mono.just(courtCase));

        final var updatedCourtCase = courtCaseService.findHearing(aCase).block();

        assertThat(updatedCourtCase.getCourtRoom()).isEqualTo("2");
        verify(courtCaseServiceClient).getHearing(COURT_CODE, CASE_NO, LIST_NO);
    }

    @DisplayName("Incoming Common Platform case returned if exist")
    @Test
    void givenExistingCommonPlatformCase_whenGetCourtCase_thenReturn() {
        final var aCase = buildCaseNoMatches();

        final var courtCase = Hearing.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(COURT_CODE)
                        .courtRoom("2")
                        .build()))
                .defendants(defendants)
                .caseId(CASE_ID)
                .hearingId(HEARING_ID)
                .source(DataSource.COMMON_PLATFORM)
                .build();

        when(courtCaseServiceClient.getHearing(HEARING_ID, CASE_ID)).thenReturn(Mono.just(courtCase));

        final var updatedCourtCase = courtCaseService.findHearing(aCase).block();

        assertThat(updatedCourtCase.getCourtRoom()).isEqualTo("2");
        verify(courtCaseServiceClient).getHearing(HEARING_ID, CASE_ID);
    }

    @DisplayName("Get court case which is new, return a null")
    @Test
    void givenNewCase_whenGetCourtCase_thenReturnNull() {
        var aCase = buildCaseNoMatches()
                .withSource(DataSource.LIBRA);

        when(courtCaseServiceClient.getHearing(COURT_CODE, CASE_NO, LIST_NO)).thenReturn(Mono.empty());

        final var newCourtCase = courtCaseService.findHearing(aCase).block();

        verify(courtCaseServiceClient).getHearing(COURT_CODE, CASE_NO, LIST_NO);
        assertThat(newCourtCase).isNull();
    }


    @DisplayName("Do not save a search responses if case put fails.")
    @Test
    void givenSearchResponse_whenCreateCourtCaseFails_thenPostMatches() {
        final var courtCase = Hearing.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(COURT_CODE)
                        .build()))
                .defendants(defendants)
                .caseId(CASE_ID)
                .hearingId(HEARING_ID)
                .caseNo(CASE_NO)
                .build();
        when(courtCaseServiceClient.putHearing(courtCase)).thenThrow(new RuntimeException("bang!"));

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> courtCaseService.saveHearing(courtCase))
                .withMessage("bang!");

        verify(courtCaseServiceClient).putHearing(courtCase);
        verifyNoMoreInteractions(courtCaseServiceClient);
    }

    @DisplayName("Fetch and update probation status")
    @Test
    void whenUpdateProbationStatus_thenMergeAndReturn() {

        var localDate = LocalDate.of(2020, Month.AUGUST, 20);
        var courtCase = Hearing.builder()
                .caseNo(CASE_NO)
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(COURT_CODE)
                        .build()))
                .defendants(List.of(Defendant.builder()
                        .crn(CRN)
                        .defendantId(DEFENDANT_UUID_1)
                        .probationStatus("PREVIOUSLY_KNOWN")
                        .build(),
                        Defendant.builder()
                                .crn(CRN_2)
                                .defendantId(DEFENDANT_UUID_2)
                                .probationStatus("NOT_SENTENCED")
                                .build()
                ))
                .build();
        var probationStatusDetail = ProbationStatusDetail.builder()
                .status("CURRENT")
                .preSentenceActivity(true)
                .build();
        var probationStatusDetail2 = ProbationStatusDetail.builder()
                .status("PREVIOUSLY_KNOWN")
                .preSentenceActivity(false)
                .awaitingPsr(true)
                .inBreach(true)
                .previouslyKnownTerminationDate(localDate)
                .build();
        var searchResponse = SearchResponse.builder().probationStatusDetail(probationStatusDetail).build();
        var searchResponse2 = SearchResponse.builder().probationStatusDetail(probationStatusDetail2).build();

        when(offenderSearchRestClient.search(CRN)).thenReturn(Mono.just(new SearchResponses(List.of(searchResponse))));
        when(offenderSearchRestClient.search(CRN_2)).thenReturn(Mono.just(new SearchResponses(List.of(searchResponse2))));

        var courtCaseResult = courtCaseService.updateProbationStatusDetail(courtCase).block();

        final var firstDefendant = courtCaseResult.getDefendants().get(0);
        assertThat(firstDefendant.getProbationStatus()).isEqualTo("CURRENT");
        assertThat(firstDefendant.getPreviouslyKnownTerminationDate()).isNull();
        assertThat(firstDefendant.getBreach()).isNull();
        assertThat(firstDefendant.getPreSentenceActivity()).isTrue();

        final var secondDefendant = courtCaseResult.getDefendants().get(1);
        assertThat(secondDefendant.getProbationStatus()).isEqualTo("PREVIOUSLY_KNOWN");
        assertThat(secondDefendant.getPreSentenceActivity()).isFalse();
        assertThat(secondDefendant.getAwaitingPsr()).isTrue();
        assertThat(secondDefendant.getPreviouslyKnownTerminationDate()).isEqualTo(localDate);

        verify(offenderSearchRestClient).search(CRN);
        verify(offenderSearchRestClient).search(CRN_2);
    }

    @DisplayName("Given more than one SearchResponse from offender search, ignore and keep probation status as-is")
    @Test
    void givenMultipleSearchResponses_whenUpdateProbationStatus_thenIgnore() {

        var localDate = LocalDate.of(2020, Month.AUGUST, 20);
        var courtCase = Hearing.builder()
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

        assertThat(courtCaseResult.getDefendants().get(0).getProbationStatus()).isEqualTo("PREVIOUSLY_KNOWN");
        assertThat(courtCaseResult.getDefendants().get(0).getPreviouslyKnownTerminationDate()).isNull();
        assertThat(courtCaseResult.getDefendants().get(0).getBreach()).isNull();
        assertThat(courtCaseResult.getDefendants().get(0).getPreSentenceActivity()).isNull();
        verify(offenderSearchRestClient).search(CRN);
    }

    @DisplayName("When rest client fails to fetch updated probation status then return the original")
    @Test
    void givenFailedCallToRestClient_whenUpdateProbationStatus_thenReturnInput() {

        final var courtCase = Hearing.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .crn(CRN)
                        .build()))
                .build();
        when(offenderSearchRestClient.search(CRN)).thenReturn(Mono.empty());

        Hearing hearingResult = courtCaseService.updateProbationStatusDetail(courtCase).block();

        assertThat(hearingResult).isEqualTo(courtCase);
        verify(offenderSearchRestClient).search(CRN);
    }

    private Hearing buildCaseNoMatches() {

        final var defendant2 = Defendant.builder().defendantId(DEFENDANT_UUID_2).build();
        return Hearing.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(COURT_CODE)
                        .courtRoom(COURT_ROOM)
                        .listNo(LIST_NO)
                        .build()))
                .defendants(List.of(DEFENDANT, defendant2))
                .caseNo(CASE_NO)
                .caseId(CASE_ID)
                .hearingId(HEARING_ID)
                .source(DataSource.COMMON_PLATFORM)
                .build();
    }

    @Test
    public void whenSearchSuccess_thenUpdateProbationStatusAndEmitEvent() {
        final var probationStatusDetail = ProbationStatusDetail.builder()
                .status(EXPECTED_PROBATION_STATUS)
                .awaitingPsr(false)
                .preSentenceActivity(false)
                .build();
        final var searchResponses = SearchResponses.builder()
                .searchResponses(List.of(SearchResponse.builder()
                        .probationStatusDetail(probationStatusDetail)
                        .build()))
                .build();
        final var defendant = Defendant.builder().crn(CRN).build();
        final var expectedDefendant = defendant.withProbationStatus(EXPECTED_PROBATION_STATUS)
                .withAwaitingPsr(false)
                .withPreSentenceActivity(false);

        when(offenderSearchRestClient.search(CRN)).thenReturn(Mono.just(searchResponses));
        final var updatedDefendant = courtCaseService.updateDefendant(defendant).block();

        AssertionsForClassTypes.assertThat(updatedDefendant.getProbationStatus()).isEqualTo(EXPECTED_PROBATION_STATUS);
        verify(telemetryService).trackDefendantProbationStatusUpdatedEvent(expectedDefendant);
    }

    @Test
    public void whenSearchReturnsEmpty_thenReturnExistingStatus() {
        final var defendant = Defendant.builder().crn(CRN).probationStatus(EXPECTED_PROBATION_STATUS).build();

        when(offenderSearchRestClient.search(CRN)).thenReturn(Mono.empty());
        final var updatedDefendant = courtCaseService.updateDefendant(defendant).block();

        AssertionsForClassTypes.assertThat(updatedDefendant.getProbationStatus()).isEqualTo(EXPECTED_PROBATION_STATUS);
        verify(telemetryService).trackDefendantProbationStatusNotUpdatedEvent(defendant);
    }


}
