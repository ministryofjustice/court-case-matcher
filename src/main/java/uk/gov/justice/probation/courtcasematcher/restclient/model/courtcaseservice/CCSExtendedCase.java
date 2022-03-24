package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CCSExtendedCase {
    private String caseId;
    private String hearingId;
    private String caseNo;
    private List<CCSDefendant> defendants;
    private List<CCSHearingDay> hearingDays;
    private CCSDataSource source;

    public static CCSExtendedCase of(CourtCase courtCase) {
        return CCSExtendedCase.builder()
                .caseId(Optional.ofNullable(courtCase.getCaseId())
                        .orElseGet(() -> UUID.randomUUID().toString()))
                .hearingId(courtCase.getHearingId())
                .caseNo(courtCase.getCaseNo())
                .source(CCSDataSource.of(courtCase.getSource()))
                .hearingDays(courtCase.getHearingDays().stream()
                        .map(CCSHearingDay::of)
                        .collect(Collectors.toList()))
                .defendants(courtCase.getDefendants().stream()
                        .map(CCSDefendant::of)
                        .collect(Collectors.toList()))
                .build();
    }

    public CourtCase asDomain() {
        return CourtCase.builder()
                .caseId(caseId)
                .hearingId(hearingId)
                .caseNo(caseNo)
                .source(source.asDomain())
                .hearingDays(hearingDays.stream()
                        .map(CCSHearingDay::asDomain)
                        .collect(Collectors.toList())
                )
                .defendants(defendants.stream()
                        .map(CCSDefendant::asDomain)
                        .collect(Collectors.toList())
                )
                .build();
    }
}
