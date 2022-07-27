package uk.gov.justice.probation.courtcasematcher.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.mapper.HearingMapper;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchRequest;

import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor(onConstructor_ = @Autowired)
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class MatcherService {

    private final OffenderSearchRestClient offenderSearchRestClient;

    private final MatchRequest.Factory matchRequestFactory;

    private TelemetryService telemetryService;

    public Mono<Hearing> matchDefendants(Hearing hearing) {

        return Mono.just(hearing.getDefendants()
                        .stream()
                        .map(defendant -> defendant.shouldMatchToOffender() ? matchDefendant(defendant, hearing) : Mono.just(defendant))
                        .map(Mono::block)
                        .collect(Collectors.toList())
                )
                .map(hearing::withDefendants)
                ;
    }

    private Mono<Defendant> matchDefendant(Defendant defendant, Hearing hearing) {
        return Mono.just(defendant)
                .map(firstDefendant -> matchRequestFactory.buildFrom(firstDefendant))
                .doOnError(e ->
                        log.warn(String.format("Unable to create MatchRequest for defendantId: %s", defendant.getDefendantId()), e))
                .flatMap(offenderSearchRestClient::match)

                .doOnSuccess(searchResponse -> log.info(String.format("Match results for defendantId: %s - matchedBy: %s, matchCount: %s",
                        defendant.getDefendantId(), searchResponse.getMatchedBy(), searchResponse.getMatches() == null ? "null" : searchResponse.getMatches().size())))
                .doOnSuccess(result -> telemetryService.trackOffenderMatchEvent(defendant, hearing, result))
                .doOnError(throwable -> {
                    log.error(throwable.getMessage());
                    telemetryService.trackOffenderMatchFailureEvent(defendant, hearing);
                })
                .map(matchResponse -> HearingMapper.updateDefendantWithMatches(defendant, matchResponse))
                ;
    }

}
