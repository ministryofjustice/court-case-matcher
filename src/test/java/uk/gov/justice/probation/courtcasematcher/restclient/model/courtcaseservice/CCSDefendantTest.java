package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.PhoneNumber;
import uk.gov.justice.probation.courtcasematcher.pact.DomainDataHelper;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CCSDefendantTest {
    @Test
    public void map() {
        var defendant = buildDefendant();

        final var actual = CCSDefendant.of(defendant);
        assertThat(actual.getDefendantId()).isEqualTo(DomainDataHelper.DEFENDANT_ID);
        assertThat(actual.getOffences().get(0)).isEqualTo(CCSOffence.builder()
                .offenceTitle("offence title")
                .offenceSummary("offence summary")
                .act("offence act")
                .sequenceNumber(1)
                .build());
        assertThat(actual.getName()).isEqualTo(CCSName.builder()
                .title("title")
                .forename1("forename 1")
                .forename2("forename 2")
                .forename3("forename 3")
                .surname("surname")
                .build());
        assertThat(actual.getAddress()).isEqualTo(CCSAddress.builder()
                .line1("line1")
                .line2("line2")
                .line3("line3")
                .line4("line4")
                .line5("line5")
                .postcode("S1 1AB")
                .build());
        assertThat(actual.getDateOfBirth()).isEqualTo(defendant.getDateOfBirth());
        assertThat(actual.getType()).isEqualTo(CCSDefendantType.ORGANISATION);

        assertThat(actual.getProbationStatus()).isEqualTo("CURRENT");
        assertThat(actual.getCrn()).isEqualTo("CRN");
        assertThat(actual.getPnc()).isEqualTo("PNC");
        assertThat(actual.getCro()).isEqualTo("CRO");
        assertThat(actual.getPreSentenceActivity()).isEqualTo(true);
        assertThat(actual.getPreviouslyKnownTerminationDate()).isEqualTo(LocalDate.of(2021, 10, 5));
        assertThat(actual.getSex()).isEqualTo("MALE");
        assertThat(actual.getSuspendedSentenceOrder()).isEqualTo(true);
        assertThat(actual.getAwaitingPsr()).isEqualTo(true);
        assertThat(actual.getBreach()).isEqualTo(true);
    }

    private Defendant buildDefendant() {
        return DomainDataHelper.aCourtCaseWithAllFields().getDefendants().get(0)
                .withProbationStatus("CURRENT")
                .withCrn("CRN")
                .withPnc("PNC")
                .withCro("CRO")
                .withPreSentenceActivity(true)
                .withPreviouslyKnownTerminationDate(LocalDate.of(2021, 10, 5))
                .withSex("M")
                .withSuspendedSentenceOrder(true)
                .withAwaitingPsr(true)
                .withBreach(true)
                .withPhoneNumber(PhoneNumber.builder()
                        .home("01000000007")
                        .work("01000000008")
                        .mobile("07000000009")
                        .build());
    }

    @Test
    public void mapBack() {
        final var original = buildDefendant();

        final var actual = CCSDefendant.of(original).asDomain();
        assertThat(actual).isEqualTo(original);
    }
}
