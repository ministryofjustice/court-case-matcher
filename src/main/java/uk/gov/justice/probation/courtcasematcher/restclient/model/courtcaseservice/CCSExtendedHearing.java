package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;

import java.util.List;
import java.util.stream.Collectors;

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CCSExtendedHearing {
    private String caseId;
    private String hearingId;
    private String caseNo;
    private String urn;
    private List<CCSDefendant> defendants;
    private List<CCSHearingDay> hearingDays;
    private CCSDataSource source;

    public static CCSExtendedHearing of(Hearing hearing) {
        return CCSExtendedHearing.builder()
                .caseId(hearing.getCaseId())
                .hearingId(hearing.getHearingId())
                .urn(hearing.getUrn())
                .caseNo(hearing.getCaseNo())
                .source(CCSDataSource.of(hearing.getSource()))
                .hearingDays(hearing.getHearingDays().stream()
                        .map(CCSHearingDay::of)
                        .collect(Collectors.toList()))
                .defendants(hearing.getDefendants().stream()
                        .map(CCSDefendant::of)
                        .collect(Collectors.toList()))
                .build();
    }

    public Hearing asDomain() {
        return Hearing.builder()
                .caseId(caseId)
                .hearingId(hearingId)
                .urn(getUrn())
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
