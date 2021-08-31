package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CCSHearingDay {
    private String courtCode;
    private String courtRoom;
    private LocalDateTime sessionStartTime;
}
