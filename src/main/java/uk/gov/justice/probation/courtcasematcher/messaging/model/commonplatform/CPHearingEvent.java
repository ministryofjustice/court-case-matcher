package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.messaging.CprExtractor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;

import java.util.List;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
public class CPHearingEvent {
    @NotNull
    @Valid
    private final CPHearing hearing;

    public List<Hearing> asDomain(CprExtractor cprExtractor) {
        return hearing.asDomain(cprExtractor);
    }
}
