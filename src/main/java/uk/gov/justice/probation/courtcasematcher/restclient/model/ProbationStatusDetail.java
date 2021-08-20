package uk.gov.justice.probation.courtcasematcher.restclient.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class ProbationStatusDetail {
    private final String status;
    private final LocalDate previouslyKnownTerminationDate;
    private final Boolean inBreach;
    private final boolean preSentenceActivity;
    private final boolean awaitingPsr;
}
