package uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class Match {
    private final OSOffender offender;

    @Setter
    private Double matchProbability;
}
