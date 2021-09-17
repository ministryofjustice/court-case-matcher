package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
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

    public CourtCase asDomain() {
        return CourtCase.builder()
                .caseId(prosecutionCases.get(0).getId())
                .source(DataSource.COMMON_PLATFORM)
                .hearingDays(hearingDays.stream()
                        .map(hearingDay -> HearingDay.builder()
                                .courtCode(courtCentre.getNormalisedCode())
                                .courtRoom(courtCentre.getRoomName())
                                .listNo(String.valueOf(hearingDay.getListingSequence()))
                                .sessionStartTime(hearingDay.getSittingDay())
                                .build())
                        .collect(Collectors.toList()))
                .defendants(prosecutionCases.get(0).getDefendants()
                        .stream()
                        .map(CPDefendant::asDomain)
                        .collect(Collectors.toList()))
                .build();
    }
}
