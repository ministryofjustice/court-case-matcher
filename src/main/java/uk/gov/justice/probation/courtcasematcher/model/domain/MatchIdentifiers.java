package uk.gov.justice.probation.courtcasematcher.model.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OffenderAlias;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class MatchIdentifiers {
    @NotNull
    private final String crn;
    private final String pnc;
    private final String cro;
    private final List<OffenderAlias> aliases;
}
