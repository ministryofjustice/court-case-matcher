package uk.gov.justice.probation.courtcasematcher.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.application.FeatureFlags;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.mapper.HearingMapper;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.PersonMatchScoreRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.PersonRecordServiceClient;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchRequest;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OSOffender;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personmatchscore.PersonMatchScoreParameter;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personmatchscore.PersonMatchScoreRequest;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personrecordservice.Person;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personrecordservice.PersonSearchRequest;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Service
@Slf4j
@AllArgsConstructor(onConstructor_ = @Autowired)
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class MatcherService {

    private final OffenderSearchRestClient offenderSearchRestClient;

    private final PersonMatchScoreRestClient personMatchScoreRestClient;

    private final PersonRecordServiceClient personRecordServiceClient;

    private final MatchRequest.Factory matchRequestFactory;

    private TelemetryService telemetryService;

    private final FeatureFlags featureFlags;

    public Mono<Hearing> matchDefendants(Hearing hearing) {
        return Mono.just(hearing.getDefendants()
                        .stream()
                        .map(defendant -> this.matchPersonAndSetPersonRecordId(defendant, hearing))
                        .map(defendant -> defendant.shouldMatchToOffender() ? matchDefendant(defendant, hearing) : Mono.just(defendant))
                        .map(Mono::block)
                        .collect(Collectors.toList())
                )
                .map(hearing::withDefendants);
    }

    public Defendant matchPersonAndSetPersonRecordId(Defendant defendant, Hearing hearing) {
        if (featureFlags.getFlag("save_person_id_to_court_case_service")) {
            var personSearchResponse = personRecordServiceClient.search(PersonSearchRequest.of(defendant))
                    .doOnError(throwable -> {
                        log.error("Unable to search a person in person record service", throwable);
                    })
                    .block();

            if (isExactPersonRecord(personSearchResponse)) {
                log.info("Successfully found an exact match in Person Record service");

                defendant.setPersonId(personSearchResponse.get(0).getPersonId().toString());
            } else {

                Person person = Person.from(defendant);
                Person createdPerson = personRecordServiceClient.createPerson(person)
                        .doOnError(throwable -> {
                            log.error("Unable to create person in person record service", throwable);
                        })
                        .block();

                log.info("Successfully created person in Person Record service");
                defendant.setPersonId(Optional.ofNullable(createdPerson).map(Person::getPersonIdString).orElse(null));
                telemetryService.trackPersonRecordCreatedEvent(defendant, hearing);

            }
            return defendant;
        }
        return defendant;

    }

    public Mono<Defendant> matchDefendant(Defendant defendant, Hearing hearing) {
        var sourceDataset = PersonMatchScoreParameter.of(hearing.getSource().name(), "DELIUS");
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
                }).map(response -> {
                    if (response.getMatchCount() > 0)
                        return response.withMatches(enrichMatchList(defendant, matchRequestFactory.buildFrom(defendant), sourceDataset, response));
                    else
                        return response;
                })
                .map(matchResponse -> HearingMapper.updateDefendantWithMatches(defendant, matchResponse));
    }

    private List<Match> enrichMatchList(Defendant defendant, MatchRequest matchRequest, PersonMatchScoreParameter sourceDataset, MatchResponse response) {
        return response.getMatches()
                .stream()
                .map(match -> enrichMatchScore(match, buildPersonMatchScoreRequest(defendant, matchRequest, sourceDataset, match.getOffender()), defendant))
                .collect(Collectors.toList());
    }

    private Match enrichMatchScore(Match match, PersonMatchScoreRequest request, Defendant defendant) {
        var probability = personMatchScoreRestClient.match(request)
                .map(personMatchScoreResponse -> personMatchScoreResponse.getMatchProbability().getValue0())
                .onErrorComplete(throwable ->
                        {
                            log.warn("Error occurred while getting person match score for defendant id {}", defendant.getDefendantId(), throwable);
                            return true;
                        }
                );
        return match.withMatchProbability(probability);
    }


    private static PersonMatchScoreRequest buildPersonMatchScoreRequest(Defendant defendant, MatchRequest matchRequest, PersonMatchScoreParameter sourceDataSet, OSOffender osOffender) {

        return PersonMatchScoreRequest.builder()
                .firstName(PersonMatchScoreParameter.of(matchRequest.getFirstName(), osOffender.getFirstName()))
                .surname(PersonMatchScoreParameter.of(matchRequest.getSurname(), osOffender.getSurname()))
                .dateOfBirth(PersonMatchScoreParameter.of(matchRequest.getDateOfBirth(), Optional.ofNullable(defendant.getDateOfBirth()).map(dob -> dob.format(DateTimeFormatter.ISO_DATE)).orElse(null)))
                .uniqueId(PersonMatchScoreParameter.of(defendant.getDefendantId(), defendant.getDefendantId()))
                .pnc(PersonMatchScoreParameter.of(matchRequest.getPncNumber(), Optional.ofNullable(osOffender.getOtherIds()).map(o -> o.getPncNumber()).orElse(null)))
                .sourceDataset(sourceDataSet)
                .build();
    }


    private Defendant setPersonRecordId(Defendant defendant) {
        if (featureFlags.getFlag("save_person_id_to_court_case_service")) {
            var personSearchResponse = personRecordServiceClient.search(PersonSearchRequest.of(defendant))
                    .doOnError(throwable -> {
                        log.error("Unable to search a person in person record service", throwable);
                    })
                    .block();

            if (isExactPersonRecord(personSearchResponse)) {
                defendant.setPersonId(personSearchResponse.get(0).getPersonId().toString());
            }
        }
        return defendant;

    }

    private boolean isExactPersonRecord(List<Person> personSearchResponse) {
        return nonNull(personSearchResponse) && personSearchResponse.size() == 1;
    }


}
