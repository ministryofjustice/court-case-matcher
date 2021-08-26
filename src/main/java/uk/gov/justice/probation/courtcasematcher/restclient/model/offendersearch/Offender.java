package uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.justice.probation.courtcasematcher.model.domain.ProbationStatusDetail;

@Getter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class Offender {
    private final OtherIds otherIds;
    private final ProbationStatusDetail probationStatus;
}
