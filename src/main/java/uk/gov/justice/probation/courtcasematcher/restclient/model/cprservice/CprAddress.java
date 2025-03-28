package uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@EqualsAndHashCode
public class CprAddress {
    private String noFixedAbode;
    private String startDate;
    private String endDate;
    private String postcode;
    private String subBuildingName;
    private String buildingName;
    private String buildingNumber;
    private String thoroughfareName;
    private String dependentLocality;
    private String postTown;
    private String county;
    private String country;
    private String uprn;
}
