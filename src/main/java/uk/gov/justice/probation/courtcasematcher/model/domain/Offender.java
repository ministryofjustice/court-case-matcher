package uk.gov.justice.probation.courtcasematcher.model.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Offender {
    private final String pnc;
}
