package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Address;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CPDefendant {
    @NotBlank
    private final String id;
    private final String pncId;
    private final String croNumber;
    @NotBlank
    private final String prosecutionCaseId;
    @NotNull
    @Valid
    private final List<CPOffence> offences;
    @Valid
    private final CPPersonDefendant personDefendant;
    @Valid
    private final CPLegalEntityDefendant legalEntityDefendant;

    public Defendant asDomain() {
        if (personDefendant == null && legalEntityDefendant == null) {
            throw new IllegalStateException(String.format("Defendant with id '%s' is neither a person nor a legal entity", getId()));
        }

        return Optional.of(this)
                .map(CPDefendant::getPersonDefendant)
                .map(CPPersonDefendant::getPersonDetails)
                .map(personDetails -> commonFieldsBuilder()
                        .type(DefendantType.PERSON)
                        .dateOfBirth(personDetails.getDateOfBirth())
                        .name(personDetails.asName())
                        .sex(personDetails.getGender())
                        .address(Optional.ofNullable(personDetails.getAddress())
                                .map(CPAddress::asDomain).orElse(Address.builder().build()))
                        .build())
                .orElseGet(() -> commonFieldsBuilder()
                            .type(DefendantType.ORGANISATION)
                            .name(getLegalEntityDefendant()
                                    .getOrganisation()
                                    .asName())
                            .build());
    }

    private Defendant.DefendantBuilder commonFieldsBuilder() {
        return Defendant.builder()
                .defendantId(getId())
                .pnc(getPncId())
                .cro(getCroNumber())
                .offences(getOffences().stream()
                        .map(CPOffence::asDomain)
                        .collect(Collectors.toList()));
    }
}
