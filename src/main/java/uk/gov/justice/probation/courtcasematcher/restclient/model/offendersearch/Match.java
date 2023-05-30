package uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch;

import lombok.*;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personrecordservice.Person;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class Match {
    private final OSOffender offender;

    @With
    @Builder.Default
    private  final Mono<Double> matchProbability = Mono.empty();
}
