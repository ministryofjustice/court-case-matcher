package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class Offence {
    @NotBlank
    private final String id;
    @NotBlank
    private final String offenceLegislation;
    @NotBlank
    private final String offenceTitle;
    @NotBlank
    private final String wording;
}
