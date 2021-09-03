package uk.gov.justice.probation.courtcasematcher.restclient.model;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.pact.DomainDataHelper;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.CCSCourtCase;

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
}
