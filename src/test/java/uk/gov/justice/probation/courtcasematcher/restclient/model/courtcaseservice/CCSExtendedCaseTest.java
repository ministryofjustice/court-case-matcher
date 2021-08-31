package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.pact.DomainDataHelper;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class CCSExtendedCaseTest {
    @Test
    public void shouldMapFromCourtCase() {
        final var courtCase = DomainDataHelper.aCourtCaseWithAllFields();
        final var actual = CCSExtendedCase.of(courtCase);

        assertThat(actual).isNotNull();
        assertThat(actual.getCaseId()).isEqualTo(courtCase.getCaseId());

        assertThat(actual.getHearingDays()).asList().containsExactly(CCSHearingDay.builder()
                        .courtCode("B10JQ")
                        .courtRoom("ROOM 1")
                        .sessionStartTime(LocalDateTime.of(2021, 8, 26, 9, 0))
                .build());
        assertThat(actual.getDefendants()).asList().containsExactly(CCSDefendant.builder()
                        .name(CCSName.builder()
                                .title("title")
                                .forename1("forename 1")
                                .forename2("forename 2")
                                .forename3("forename 3")
                                .surname("surname")
                                .build())

                        .dateOfBirth(LocalDate.of(1986, 11, 28))
                        .address(CCSAddress.builder()
                                .line1("line1")
                                .line2("line2")
                                .line3("line3")
                                .line4("line4")
                                .line5("line5")
                                .postcode("S1 1AB")
                                .build())
                        .type(CCSDefendantType.PERSON)
                        .probationStatus("Current")
                        .build()
        );
    }
}
