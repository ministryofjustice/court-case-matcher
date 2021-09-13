package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class Defendant {
    private final String id;
    private final String pncId;
    private final String croNumber;
    private final String prosecutionCaseId;
    private final List<Offence> offences;
    private final PersonDefendant personDefendant;
    private final LegalEntityDefendant legalEntityDefendant;
}
