package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.CaseMarker;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CCSCaseMarker {
    private String markerTypeDescription;

    public static CCSCaseMarker of(CaseMarker caseMarker) {
        return CCSCaseMarker.builder()
                .markerTypeDescription(caseMarker.getMarkerTypeDescription())
                .build();
    }

    public CaseMarker asDomain() {
        return CaseMarker.builder()
                .markerTypeDescription(markerTypeDescription)
                .build();
    }
}
