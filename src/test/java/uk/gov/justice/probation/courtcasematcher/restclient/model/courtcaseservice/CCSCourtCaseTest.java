package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.pact.DomainDataHelper;

import static org.assertj.core.api.Assertions.assertThat;

class CCSCourtCaseTest {

    @Test
    public void map() {
        final var courtCase = DomainDataHelper.aCourtCaseWithAllFields();

        final var courtCaseRequest = CCSCourtCase.of(courtCase);

        assertThat(courtCaseRequest).usingRecursiveComparison()
                .ignoringFields("probationStatusActual")
                .isEqualTo(courtCase);
    }

    @Test
    public void mapBack() {
        final var original = DomainDataHelper.aCourtCaseWithAllFields();

        final var actual = CCSCourtCase.of(original).asDomain();

        assertThat(actual).isEqualTo(original);
    }

    @Test
    public void isPerson() {
        final var courtCase = CCSCourtCase.builder()
                .defendantType(CCSDefendantType.PERSON)
                .build();
        assertThat(courtCase.isPerson()).isTrue();
    }

    @Test
    public void isNotPerson() {
        final var courtCase = CCSCourtCase.builder()
                .defendantType(CCSDefendantType.ORGANISATION)
                .build();
        assertThat(courtCase.isPerson()).isFalse();
    }

    @Test
    public void isNullPerson() {
        final var courtCase = CCSCourtCase.builder()
                .defendantType(null)
                .build();
        assertThat(courtCase.isPerson()).isFalse();
    }

}
