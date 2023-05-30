package uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;
import reactor.core.publisher.Mono;

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
