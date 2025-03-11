package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.messaging.CprExtractor;
import uk.gov.justice.probation.courtcasematcher.model.domain.CaseMarker;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Objects;
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


    //Pass in the cprExtractor into this method
    public List<Hearing> asDomain(CprExtractor cprExtractor) {
        return prosecutionCases.stream().map(cpProsecutionCase -> Hearing.builder()
            .caseId(cpProsecutionCase.getId())
            .source(DataSource.COMMON_PLATFORM)
            .hearingDays(hearingDays.stream()
                .map(hearingDay -> HearingDay.builder()
                    .courtCode(courtCentre.getNormalisedCode())
                    .courtRoom(courtCentre.getRoomName())
                    .sessionStartTime(hearingDay.getSittingDay())
                    .build())
                .collect(Collectors.toList()))
            .defendants(cpProsecutionCase.getDefendants()
                .stream()
                .map(cpDefendant -> cpDefendant.asDomain(cprExtractor.canExtractCprFields(courtCentre.getNormalisedCode())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()))
            .caseMarkers(buildCaseMarkers(cpProsecutionCase))
            .urn(cpProsecutionCase.getProsecutionCaseIdentifier().getCaseUrn())
            .hearingType(Optional.ofNullable(type).map(CPHearingType::getDescription).orElse(null))
            .build()
        ).toList();
    }

    private List<CaseMarker> buildCaseMarkers(CPProsecutionCase prosecutionCase) {
        if(prosecutionCase.getCaseMarkers() != null) {
            return prosecutionCase.getCaseMarkers()
                    .stream()
                    .map(cpCaseMarker -> CaseMarker.builder()
                            .markerTypeDescription(cpCaseMarker.getMarkerTypeDescription())
                            .build())
                    .collect(Collectors.toList());
        }
        return null;
    }
}
