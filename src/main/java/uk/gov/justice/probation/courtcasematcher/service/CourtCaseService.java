package uk.gov.justice.probation.courtcasematcher.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.mapper.HearingMapper;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseServiceClient;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;

import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor(onConstructor_ = @Autowired)
@NoArgsConstructor
public class CourtCaseService {

    private CourtCaseServiceClient courtCaseServiceClient;

    private OffenderSearchRestClient offenderSearchRestClient;

    private TelemetryService telemetryService;

    public Mono<Hearing> findHearing(Hearing hearing) {
        if (hearing.getSource() == DataSource.COMMON_PLATFORM) {
            return courtCaseServiceClient.getHearing(hearing.getHearingId(), hearing.getCaseId());
        }
        return courtCaseServiceClient.getHearing(hearing.getCourtCode(), hearing.getCaseNo(), hearing.getListNo());
    }

    public void saveHearing(Hearing hearing) {
        Hearing updatedHearing = hearing;

        // If this is a new case from COMMON platform, set caseNo = caseId
        if (hearing.getSource() == DataSource.COMMON_PLATFORM && hearing.getCaseNo() == null) {
            updatedHearing = hearing.withCaseNo(updatedHearing.getCaseId());
        }

        courtCaseServiceClient.putHearing(updatedHearing)
                .doOnError(throwable -> {
                    log.error("Save court case failed for case id {} with {}", hearing.getCaseId(), throwable.getMessage());
                    throw new RuntimeException(throwable.getMessage());
                })
                .block();

        courtCaseServiceClient.postOffenderMatches(updatedHearing.getCaseId(), updatedHearing.getDefendants())
                .block();

    }
    public Mono<Hearing> updateProbationStatusDetail(Hearing hearing) {
        final var updatedDefendants = hearing.getDefendants()
                .stream()
                .map(defendant -> defendant.getCrn() != null && defendant.getCprUUID() == null ? updateDefendant(defendant) : Mono.just(defendant))
                .map(Mono::block)
                .collect(Collectors.toList());

        return Mono.just(hearing.withDefendants(updatedDefendants));
    }

    public Mono<Defendant> updateDefendant(Defendant defendant) {
        return offenderSearchRestClient.search(defendant.getCrn())
                .filter(searchResponses -> searchResponses.getSearchResponses().size() == 1)
                .map(searchResponses -> searchResponses.getSearchResponses().getFirst().getProbationStatusDetail())
                .map(probationStatusDetail -> HearingMapper.merge(probationStatusDetail, defendant))
                .doOnSuccess((updatedDefendant) -> telemetryService.trackDefendantProbationStatusUpdatedEvent(updatedDefendant))
                .switchIfEmpty(
                        Mono.just(defendant)
                        .doOnSuccess((ignored) -> telemetryService.trackDefendantProbationStatusNotUpdatedEvent(defendant))
                );
    }
}
