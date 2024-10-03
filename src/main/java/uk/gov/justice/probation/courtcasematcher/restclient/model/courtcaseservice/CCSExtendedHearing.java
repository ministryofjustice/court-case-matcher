package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.CaseMarker;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
    private String hearingEventType;

    private String hearingType;

    private List<CCSCaseMarker> caseMarkers;
    private LocalDateTime lastUpdated;

    public static CCSExtendedHearing of(Hearing hearing) {
        return CCSExtendedHearing.builder()
                .caseId(hearing.getCaseId())
                .hearingId(hearing.getHearingId())
                .hearingType(hearing.getHearingType())
                .urn(hearing.getUrn())
                .caseNo(hearing.getCaseNo())
                .source(CCSDataSource.of(hearing.getSource()))
                .hearingEventType(hearing.getHearingEventType())
                .hearingDays(hearing.getHearingDays().stream()
                        .map(CCSHearingDay::of)
                        .collect(Collectors.toList()))
                .caseMarkers(getCCSCaseMarkersIfExist(hearing))
                .defendants(hearing.getDefendants().stream()
                        .map(CCSDefendant::of)
                        .collect(Collectors.toList()))
                .build();
    }

    private static List<CCSCaseMarker> getCCSCaseMarkersIfExist(Hearing hearing) {
        return Optional.ofNullable(hearing.getCaseMarkers())
                .map(caseMarkersList ->
                        caseMarkersList.stream()
                                .map(CCSCaseMarker::of)
                                .collect(Collectors.toList())).orElse(null);
    }

    public Hearing asDomain() {
        return Hearing.builder()
                .caseId(caseId)
                .hearingId(hearingId)
                .urn(getUrn())
                .caseNo(caseNo)
                .source(source.asDomain())
                .hearingEventType(hearingEventType)
                .hearingType(hearingType)
                .hearingDays(hearingDays.stream()
                        .map(CCSHearingDay::asDomain)
                        .collect(Collectors.toList())
                )
                .defendants(defendants.stream()
                        .map(CCSDefendant::asDomain)
                        .collect(Collectors.toList())
                )
                .caseMarkers(getCaseMarkersIfExist())
                .lastUpdated(lastUpdated)
                .build();
    }

    private List<CaseMarker> getCaseMarkersIfExist() {
        return Optional.ofNullable(caseMarkers)
                .map(caseMarkers -> caseMarkers.stream()
                        .map(CCSCaseMarker::asDomain)
                        .collect(Collectors.toList())).orElse(null);

    }
}
