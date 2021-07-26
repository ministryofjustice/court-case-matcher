package uk.gov.justice.probation.courtcasematcher.model.gateway;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@Data
public class Offence {

    @NotNull
    @PositiveOrZero
    private final Integer seq;

    private final String summary;
    private final String title;
    private final String act;
}
