package uk.gov.justice.probation.courtcasematcher.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.mapper.HearingMapper;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.PersonMatchScoreRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchRequest;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OSOffender;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personmatchscore.PersonMatchScoreParameter;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personmatchscore.PersonMatchScoreRequest;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor(onConstructor_ = @Autowired)
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class MatcherService {

    private final OffenderSearchRestClient offenderSearchRestClient;

    private final PersonMatchScoreRestClient personMatchScoreRestClient;

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

    public Mono<Defendant> matchDefendant(Defendant defendant, Hearing hearing) {
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
                .map(matchResponse -> {
                  enrichWithMatchScore(hearing, defendant, matchResponse);
                  return matchResponse;
                })
                .map(matchResponse -> HearingMapper.updateDefendantWithMatches(defendant, matchResponse))
                ;
    }

  private void enrichWithMatchScore(Hearing hearing, Defendant defendant, MatchResponse matchResponse) {
      var matchRequest = matchRequestFactory.buildFrom(defendant);
      var sourceDataset = PersonMatchScoreParameter.of(hearing.getSource().name(), "DELIUS");
      if (matchResponse.getMatchCount() > 0) {

        matchResponse.getMatches().stream().forEach(match -> {
          PersonMatchScoreRequest request = buildPersonMatchScoreRequest(defendant, matchRequest, sourceDataset, match.getOffender());
          personMatchScoreRestClient.match(request)
            .doOnSuccess(personMatchScoreResponse -> match.setMatchProbability(personMatchScoreResponse.getMatchProbability().getValue0()))
            .onErrorComplete(throwable ->
              {
                log.warn("Error occurred while getting person match score for defendant id {}", defendant.getDefendantId(), throwable);
                return true;
              }
            )
            .block();
        });
      }
  }

  private static PersonMatchScoreRequest buildPersonMatchScoreRequest(Defendant defendant, MatchRequest matchRequest, PersonMatchScoreParameter sourceDataSet, OSOffender osOffender) {

    return PersonMatchScoreRequest.builder()
      .firstName(PersonMatchScoreParameter.of(matchRequest.getFirstName(), osOffender.getFirstName()))
      .surname(PersonMatchScoreParameter.of(matchRequest.getSurname(), osOffender.getSurname()))
      .dateOfBirth(PersonMatchScoreParameter.of(matchRequest.getDateOfBirth(), defendant.getDateOfBirth().format(DateTimeFormatter.ISO_DATE)))
      .uniqueId(PersonMatchScoreParameter.of(defendant.getDefendantId(), defendant.getDefendantId()))
      .pnc(PersonMatchScoreParameter.of(matchRequest.getPncNumber(), Optional.ofNullable(osOffender.getOtherIds()).map(o -> o.getPncNumber()).orElse(null)))
      .sourceDataset(sourceDataSet)
      .build();
  }
}
