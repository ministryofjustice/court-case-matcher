package uk.gov.justice.probation.courtcasematcher.model.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.type.MatchType;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class OffenderMatch {
    @NotNull
    @Valid
    private final MatchIdentifiers matchIdentifiers;
    @NotNull
    private final MatchType matchType;
    @NotNull
    private final Boolean confirmed;
    @NotNull
    private final Boolean rejected;
    @Builder.Default
    private final Mono<Double> matchProbability = Mono.empty();
}
