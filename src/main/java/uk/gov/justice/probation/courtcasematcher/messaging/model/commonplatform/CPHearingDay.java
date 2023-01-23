package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CPHearingDay {
    @NotNull
    private final Integer listedDurationMinutes;
    private final Integer listingSequence;
    @NotNull
    private final LocalDateTime sittingDay;
}
