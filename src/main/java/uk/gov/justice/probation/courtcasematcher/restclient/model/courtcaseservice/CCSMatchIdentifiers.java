package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.MatchIdentifiers;

import javax.validation.constraints.NotNull;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CCSMatchIdentifiers {
    @NotNull
    private final String crn;
    private final String pnc;
    private final String cro;

    public static CCSMatchIdentifiers of(MatchIdentifiers matchIdentifiers) {
        return CCSMatchIdentifiers.builder()
                .crn(matchIdentifiers.getCrn())
                .pnc(matchIdentifiers.getPnc())
                .cro(matchIdentifiers.getCro())
                .build();
    }
}
