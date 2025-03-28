package uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@EqualsAndHashCode
public class CprIdentifier {
    private List<String> crns;
    private List<String> prisonNumbers;
    private List<String> defendantIds;
    private List<String> cids;
    private List<String> pncs;
    private List<String> cros;
    private List<String> nationalInsuranceNumbers;
    private List<String> driverLicenseNumbers;
    private List<String> arrestSummonsNumbers;
}
