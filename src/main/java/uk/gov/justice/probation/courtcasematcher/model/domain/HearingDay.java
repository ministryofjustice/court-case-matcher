package uk.gov.justice.probation.courtcasematcher.model.domain;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@EqualsAndHashCode
public class HearingDay {
    private String courtCode;
    private String courtRoom;
    private String listNo;
    private LocalDateTime sessionStartTime;
}
