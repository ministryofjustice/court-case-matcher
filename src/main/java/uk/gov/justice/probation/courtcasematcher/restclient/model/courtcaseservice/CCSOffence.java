package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;
import uk.gov.justice.probation.courtcasematcher.model.domain.Plea;
import uk.gov.justice.probation.courtcasematcher.model.domain.Verdict;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CCSOffence {

    private final String offenceTitle;

    private final String offenceSummary;

    private final String act;

    private final Integer sequenceNumber;

    private final Integer listNo;

    private final String offenceCode;

    private List<CCSJudicialResult> judicialResults;

    private CCSPlea plea;

    private CCSVerdict verdict;

    public static CCSOffence of(Offence offence) {
        return CCSOffence.builder()
                .offenceTitle(offence.getOffenceTitle())
                .offenceSummary(offence.getOffenceSummary())
                .act(offence.getAct())
                .sequenceNumber(offence.getSequenceNumber())
                .listNo(offence.getListNo())
                .offenceCode(offence.getOffenceCode())
                .plea(Optional.ofNullable(offence.getPlea()).map(CCSPlea::of).orElse(null))
                .verdict(Optional.ofNullable(offence.getVerdict()).map(CCSVerdict::of).orElse(null))
                .judicialResults(Optional.of(offence)
                        .map(Offence::getJudicialResults)
                        .orElse(Collections.emptyList())
                        .stream().map(CCSJudicialResult::of)
                        .collect(Collectors.toList()))
                .build();
    }

    public Offence asDomain() {
        return Offence.builder()
                .offenceTitle(getOffenceTitle())
                .offenceSummary(getOffenceSummary())
                .act(getAct())
                .sequenceNumber(getSequenceNumber())
                .listNo(listNo)
                .offenceCode(getOffenceCode())
                .plea(Optional.ofNullable(plea).map(CCSPlea::asDomain).orElse(null))
                .verdict(Optional.ofNullable(verdict).map(CCSVerdict::asDomain).orElse(null))
                .judicialResults(Optional.ofNullable(getJudicialResults())
                        .map(judicialResults -> judicialResults.stream()
                                .map(CCSJudicialResult::asDomain)
                                .collect(Collectors.toList()))
                        .orElse(null))
                .build();
    }
}
