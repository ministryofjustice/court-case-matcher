package uk.gov.justice.probation.courtcasematcher.messaging.model.libra;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@Data
public class LibraOffence {

    @NotNull
    @PositiveOrZero
    private final Integer seq;

    private final String summary;
    private final String title;
    private final String act;
}
