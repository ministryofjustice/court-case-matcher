package uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.justice.probation.courtcasematcher.model.domain.ProbationStatusDetail;

import java.util.List;

@Getter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class OSOffender {
    private final String firstName;
    private final String surname;
    private final String dateOfBirth;
    private final OtherIds otherIds;
    private final ProbationStatusDetail probationStatus;
    private final List<OffenderAlias> offenderAliases;
}
