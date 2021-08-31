package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CCSDefendant {
    private CCSAddress address;
    private LocalDate dateOfBirth;
    private CCSName name;
    private List<CCSOffence> offences;
    private String probationStatus;
    private CCSDefendantType type;
}
