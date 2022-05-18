package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.MatchIdentifiers;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CCSMatchIdentifiers {
    @NotNull
    private final String crn;
    private final String pnc;
    private final String cro;
    private final List<CCSOffenderAlias> aliases;

    public static CCSMatchIdentifiers of(MatchIdentifiers matchIdentifiers) {
        return CCSMatchIdentifiers.builder()
                .crn(matchIdentifiers.getCrn())
                .pnc(matchIdentifiers.getPnc())
                .cro(matchIdentifiers.getCro())
                .aliases(mapAliases(matchIdentifiers))
                .build();
    }

    private static List<CCSOffenderAlias> mapAliases(MatchIdentifiers matchIdentifiers) {
        return Optional.ofNullable(matchIdentifiers.getAliases()).map(offenderAliases ->
                        offenderAliases.stream().map(CCSOffenderAlias::of).collect(Collectors.toList()))
                .orElse(null);
    }
}
