package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.CaseMarker;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
public class CPHearing {
    @NotBlank
    private final String id;
    @NotNull
    @Valid
    private final CPCourtCentre courtCentre;
    @NotEmpty
    @Valid
    private final List<CPHearingDay> hearingDays;
    @NotNull
    private final CPJurisdictionType jurisdictionType;
    @NotEmpty
    @Valid
    private final List<CPProsecutionCase> prosecutionCases;

    private final CPHearingType type;

    public Hearing asDomain() {
        return Hearing.builder()
                .caseId(prosecutionCases.get(0).getId())
                .source(DataSource.COMMON_PLATFORM)
                .hearingDays(hearingDays.stream()
                        .map(hearingDay -> HearingDay.builder()
                                .courtCode(courtCentre.getNormalisedCode())
                                .courtRoom(courtCentre.getRoomName())
                                .sessionStartTime(hearingDay.getSittingDay())
                                .build())
                        .collect(Collectors.toList()))
                .defendants(prosecutionCases.get(0).getDefendants()
                        .stream()
                        .map(CPDefendant::asDomain)
                        .collect(Collectors.toList()))
                .caseMarkers(buildCaseMarkers(prosecutionCases))
                .urn(prosecutionCases.get(0).getProsecutionCaseIdentifier().getCaseUrn())
                .hearingType(Optional.ofNullable(type).map(CPHearingType::getDescription).orElse(null))
                .build();
    }

    private List<CaseMarker> buildCaseMarkers(List<CPProsecutionCase> prosecutionCases) {
        if(prosecutionCases.get(0).getCaseMarkers() != null) {
            return prosecutionCases.get(0).getCaseMarkers()
                    .stream()
                    .map(cpCaseMarker -> CaseMarker.builder()
                            .markerTypeDescription(cpCaseMarker.getMarkerTypeDescription())
                            .build())
                    .collect(Collectors.toList());
        }
        return null;
    }
}
