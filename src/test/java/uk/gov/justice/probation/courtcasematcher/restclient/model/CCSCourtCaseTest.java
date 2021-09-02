package uk.gov.justice.probation.courtcasematcher.restclient.model;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.pact.DomainDataHelper;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.CCSCourtCase;

import static org.assertj.core.api.Assertions.assertThat;

class CCSCourtCaseTest {

    private static final String A_UUID = "8E07B58D-3ED3-440E-9CC2-2BC94EDBC5AF";

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
    public void generateUUIDsIfNull() {
        final var original = DomainDataHelper.aCourtCaseBuilderWithAllFields()
                .defendantId(null)
                .caseId(null)
                .build();

        final var actual = CCSCourtCase.of(original).asDomain();

        assertThat(actual.getDefendantId()).isNotBlank()
                .hasSameSizeAs(A_UUID);

        assertThat(actual.getCaseId()).isNotBlank()
                .hasSameSizeAs(A_UUID);
    }
}
