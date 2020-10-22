package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class MatchRequestTest {

    private static final String PNC = "PNC";
    private static final String SURNAME = "SURNAME";
    private static final String FORENAME_1 = "FORENAME1";
    private static final String FORENAME_2 = "FORENAME2";
    private static final String FORENAME_3 = "FORENAME3";
    private static final String TITLE = "MR";
    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(1980, 1, 1);

    @Test
    public void givenAllValuesProvided_shouldBuildValidRequest() {
        final var name = Name.builder()
                .forename1(FORENAME_1)
                .forename2(FORENAME_2)
                .forename3(FORENAME_3)
                .surname(SURNAME)
                .title(TITLE)
                .build();

        final var matchRequest = MatchRequest.from(PNC, name, DATE_OF_BIRTH);
        assertThat(matchRequest.getPncNumber()).isEqualTo(PNC);
        assertThat(matchRequest.getFirstName()).isEqualTo(String.format("%s %s %s", FORENAME_1, FORENAME_2, FORENAME_3));
        assertThat(matchRequest.getSurname()).isEqualTo(SURNAME);
        assertThat(matchRequest.getDateOfBirth()).isEqualTo(DATE_OF_BIRTH.format(DateTimeFormatter.ISO_DATE));
    }

    @Test
    public void givenMinimalValuesProvided_shouldBuildValidRequest() {
        final var name = Name.builder()
                .surname(SURNAME)
                .build();

        final var matchRequest = MatchRequest.from(null, name, DATE_OF_BIRTH);
        assertThat(matchRequest.getPncNumber()).isEqualTo(null);
        assertThat(matchRequest.getFirstName()).isEqualTo(null);
        assertThat(matchRequest.getSurname()).isEqualTo(SURNAME);
        assertThat(matchRequest.getDateOfBirth()).isEqualTo(DATE_OF_BIRTH.format(DateTimeFormatter.ISO_DATE));

    }

    @Test
    public void givenNoSurnameProvided_shouldThrowException() {
        final var name = Name.builder()
                .build();
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> MatchRequest.from(null, name, DATE_OF_BIRTH));

    }

    @Test
    public void givenNoDateOfBirthProvided_shouldThrowException() {
        final var name = Name.builder()
                .surname(SURNAME)
                .build();
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> MatchRequest.from(null, name, null));

    }
}