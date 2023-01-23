package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.CCSJudicialResult;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CPOffence {
    @NotBlank
    private final String id;
    private final String offenceLegislation;
    @NotBlank
    private final String offenceTitle;
    @NotBlank
    private final String wording;
    private final Integer listingNumber;

    private List<CPJudicialResult> judicialResults;

    public Offence asDomain() {
        return Offence.builder()
                .id(id)
                .act(offenceLegislation)
                .offenceTitle(offenceTitle)
                .offenceSummary(wording)
                .listNo(listingNumber)
                .judicialResults(Optional.ofNullable(getJudicialResults())
                        .map(judicialResults -> judicialResults.stream()
                                .map(CPJudicialResult::asDomain)
                                .collect(Collectors.toList()))
                        .orElse(null))
                .build();
    }
}
