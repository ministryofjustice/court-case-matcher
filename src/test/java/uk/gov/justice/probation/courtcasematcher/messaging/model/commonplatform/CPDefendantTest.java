package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

class CPDefendantTest {
    @Test
    public void person_asDomain() {
        final var actual = CPDefendant.builder()
                .personDefendant(CPPersonDefendant.builder()
                        .personDetails(CPPersonDetails.builder()
                                .gender("gender")
                                .title("title")
                                .firstName("firstname")
                                .middleName("middlename")
                                .lastName("lastname")
                                .dateOfBirth(LocalDate.of(2021, 1, 1))
                                .address(CPAddress.builder()
                                        .address1("address1")
                                        .build())
                                .build())
                        .build())
                .offences(List.of(CPOffence.builder().id("1").build(), CPOffence.builder().id("2").build()))
                .id("2B6AAC03-FEFD-41E9-87C2-7B3E8B8F27D9")
                .pncId("pncid")
                .croNumber("croNumber")
                .build()
                .asDomain();

        assertThat(actual.getDefendantId()).isEqualTo("2B6AAC03-FEFD-41E9-87C2-7B3E8B8F27D9");
        assertThat(actual.getType()).isEqualTo(DefendantType.PERSON);
        assertThat(actual.getName().getForename1()).isEqualTo("firstname");
        assertThat(actual.getDateOfBirth()).isEqualTo(LocalDate.of(2021, 1, 1));
        assertThat(actual.getSex()).isEqualTo("gender");
        assertThat(actual.getCro()).isEqualTo("croNumber");
        assertThat(actual.getPnc()).isEqualTo("pncid");
        assertThat(actual.getAddress().getLine1()).isEqualTo("address1");
        assertThat(actual.getOffences().get(0).getId()).isEqualTo("1");
        assertThat(actual.getOffences().get(1).getId()).isEqualTo("2");
    }

    @Test
    public void organisation_asDomain() {
        final var actual = CPDefendant.builder()
                .legalEntityDefendant(CPLegalEntityDefendant.builder()
                        .organisation(CPOrganisation.builder()
                                .name("orgname")
                                .build())
                        .build())
                .offences(List.of(CPOffence.builder().id("1").build(), CPOffence.builder().id("2").build()))
                .id("2B6AAC03-FEFD-41E9-87C2-7B3E8B8F27D9")
                .pncId("pncid")
                .croNumber("croNumber")
                .build()
                .asDomain();

        assertThat(actual.getDefendantId()).isEqualTo("2B6AAC03-FEFD-41E9-87C2-7B3E8B8F27D9");
        assertThat(actual.getType()).isEqualTo(DefendantType.ORGANISATION);
        assertThat(actual.getName().getSurname()).isEqualTo("orgname");
        assertThat(actual.getDateOfBirth()).isNull();
        assertThat(actual.getSex()).isNull();
        assertThat(actual.getCro()).isEqualTo("croNumber");
        assertThat(actual.getPnc()).isEqualTo("pncid");
        assertThat(actual.getAddress()).isNull();
        assertThat(actual.getOffences().get(0).getId()).isEqualTo("1");
        assertThat(actual.getOffences().get(1).getId()).isEqualTo("2");
    }

    @Test
    public void defendantWithoutPersonOrLegalEntity_throws() {
        final var defendant = CPDefendant.builder()
                .offences(List.of(CPOffence.builder().id("1").build(), CPOffence.builder().id("2").build()))
                .id("2B6AAC03-FEFD-41E9-87C2-7B3E8B8F27D9")
                .pncId("pncid")
                .croNumber("croNumber")
                .build();

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(defendant::asDomain)
                .withMessage("Defendant with id '2B6AAC03-FEFD-41E9-87C2-7B3E8B8F27D9' is neither a person nor a legal entity");

    }
}