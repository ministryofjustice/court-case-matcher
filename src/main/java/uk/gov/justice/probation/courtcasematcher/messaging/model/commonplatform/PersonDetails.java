package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class PersonDetails {
    private final String title;
    private final String firstName;
    private final String middleName;
    @NotBlank
    private final String lastName;
    @NotNull
    private final LocalDate dateOfBirth;
    @NotBlank
    private final String gender;
    @Valid
    private final Address address;
}
