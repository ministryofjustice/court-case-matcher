package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CCSOffender {
    private final String pnc;
    private final String cro;
}
