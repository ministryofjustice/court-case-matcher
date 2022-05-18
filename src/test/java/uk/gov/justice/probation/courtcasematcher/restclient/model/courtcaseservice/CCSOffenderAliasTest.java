package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OffenderAlias;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CCSOffenderAliasTest {

    @Test
    void shouldMapToCCSOffenderAlias() {
        final var offenderAlias = OffenderAlias.builder()
                .dateOfBirth(LocalDate.of(2000, 10, 01))
                .firstName("Joe")
                .surname("Bloggs")
                .middleNames(List.of("Sean", "Kane"))
                .gender("Male")
                .build();

        final CCSOffenderAlias actual = CCSOffenderAlias.of(offenderAlias);
        assertThat(actual.getGender()).isEqualTo(offenderAlias.getGender());
        assertThat(actual.getFirstName()).isEqualTo(offenderAlias.getFirstName());
        assertThat(actual.getSurname()).isEqualTo(offenderAlias.getSurname());
        assertThat(actual.getMiddleNames()).isEqualTo(offenderAlias.getMiddleNames());
        assertThat(actual.getDateOfBirth()).isEqualTo(offenderAlias.getDateOfBirth());

    }

}