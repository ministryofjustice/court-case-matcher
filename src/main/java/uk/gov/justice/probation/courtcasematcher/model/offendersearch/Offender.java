package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class Offender {
    private final OtherIds otherIds;
    private final ProbationStatus probationStatus;
}
