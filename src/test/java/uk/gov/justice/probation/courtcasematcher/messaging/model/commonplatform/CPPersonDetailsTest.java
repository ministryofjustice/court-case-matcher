package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CPPersonDetailsTest {
    @Test
    public void asName() {
        final var actual = CPPersonDetails.builder()
                .dateOfBirth(LocalDate.of(2021, 1, 1))
                .title("title")
                .firstName("first")
                .middleName("middle")
                .lastName("last")
                .gender("gender")
                .build().asName();

        assertThat(actual).isEqualTo(Name.builder()
                        .title("title")
                        .forename1("first")
                        .forename2("middle")
                        .surname("last")
                .build());
    }

}
