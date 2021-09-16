package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.pact.DomainDataHelper;

import static org.assertj.core.api.Assertions.assertThat;

class CCSCourtCaseTest {

    @Test
    public void map() {
        final var courtCase = DomainDataHelper.aCourtCaseWithAllFields();

        final var courtCaseRequest = CCSCourtCase.of(courtCase);

        assertThat(courtCaseRequest.getCourtCode()).isEqualTo(courtCase.getCourtCode());
        assertThat(courtCaseRequest.getCourtRoom()).isEqualTo(courtCase.getCourtRoom());
        assertThat(courtCaseRequest.getCaseId()).isEqualTo(courtCase.getCaseId());
        assertThat(courtCaseRequest.getCaseNo()).isEqualTo(courtCase.getCaseNo());

        assertThat(courtCaseRequest.getListNo()).isEqualTo(courtCase.getListNo());
        assertThat(courtCaseRequest.getSessionStartTime()).isEqualTo(courtCase.getSessionStartTime());

        final var firstDefendant = courtCase.getFirstDefendant();
        assertThat(courtCaseRequest.getDefendantId()).isEqualTo(firstDefendant.getDefendantId());
        assertThat(courtCaseRequest.getProbationStatusActual()).isEqualTo(firstDefendant.getProbationStatus());
        assertThat(courtCaseRequest.getOffences()).usingRecursiveComparison().isEqualTo(firstDefendant.getOffences());
        assertThat(courtCaseRequest.getCrn()).isEqualTo(firstDefendant.getCrn());
        assertThat(courtCaseRequest.getCro()).isEqualTo(firstDefendant.getCro());
        assertThat(courtCaseRequest.getPnc()).isEqualTo(firstDefendant.getPnc());
        assertThat(courtCaseRequest.getName()).usingRecursiveComparison().isEqualTo(firstDefendant.getName());
        assertThat(courtCaseRequest.getDefendantName()).isEqualTo(firstDefendant.getName().getFullName());
        assertThat(courtCaseRequest.getDefendantAddress()).isEqualTo(CCSAddress.builder()
                        .line1("line1")
                        .line2("line2")
                        .line3("line3")
                        .line4("line4")
                        .line5("line5")
                        .postcode("S1 1AB")
                .build());
        assertThat(courtCaseRequest.getDefendantDob()).isEqualTo(firstDefendant.getDateOfBirth());
        assertThat(courtCaseRequest.getDefendantType()).isEqualTo(CCSDefendantType.ORGANISATION);
        assertThat(courtCaseRequest.getDefendantSex()).isEqualTo(firstDefendant.getSex());
        assertThat(courtCaseRequest.getBreach()).isEqualTo(firstDefendant.getBreach());
        assertThat(courtCaseRequest.getPreviouslyKnownTerminationDate()).isEqualTo(firstDefendant.getPreviouslyKnownTerminationDate());
        assertThat(courtCaseRequest.getSuspendedSentenceOrder()).isEqualTo(firstDefendant.getSuspendedSentenceOrder());
        assertThat(courtCaseRequest.isPreSentenceActivity()).isEqualTo(firstDefendant.getPreSentenceActivity());
        assertThat(courtCaseRequest.isAwaitingPsr()).isEqualTo(firstDefendant.getAwaitingPsr());
        assertThat(courtCaseRequest.getSource()).isEqualTo(CCSDataSource.LIBRA);
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
