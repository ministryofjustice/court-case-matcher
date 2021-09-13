package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class Defendant {
    @NotBlank
    private final String id;
    private final String pncId;
    private final String croNumber;
    @NotBlank
    private final String prosecutionCaseId;
    @NotNull
    @Valid
    private final List<Offence> offences;
    @Valid
    private final PersonDefendant personDefendant;
    @Valid
    private final LegalEntityDefendant legalEntityDefendant;
}
