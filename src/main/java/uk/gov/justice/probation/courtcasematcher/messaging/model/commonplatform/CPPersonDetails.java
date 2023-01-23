package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CPPersonDetails {
    private final String title;
    private final String firstName;
    private final String middleName;
    @NotBlank
    private final String lastName;
    private final LocalDate dateOfBirth;
    @NotBlank
    private final String gender;
    @Valid
    private final CPAddress address;
    @Valid
    private final CPContact contact;

    public Name asName() {
        return Name.builder()
                .title(title)
                .forename1(firstName)
                .forename2(middleName)
                .surname(lastName)
                .build();
    }
}
