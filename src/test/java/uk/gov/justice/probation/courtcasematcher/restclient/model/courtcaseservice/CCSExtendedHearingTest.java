package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.pact.DomainDataHelper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class CCSExtendedHearingTest {

    @Test
    public void shouldMapFromCourtCaseAndKeepIdsIfPresent() {
        final var courtCase = DomainDataHelper.aHearingWithAllFields();
        final var actual = CCSExtendedHearing.of(courtCase);

        assertThat(actual.getCaseId()).isNotBlank().isEqualTo(courtCase.getCaseId());
        assertThat(actual.getHearingId()).isEqualTo(courtCase.getHearingId());
        assertThat(actual.getCaseNo()).isEqualTo(courtCase.getCaseNo());
        assertThat(actual.getUrn()).isEqualTo(courtCase.getUrn());
        assertThat(actual.getSource()).isEqualTo(CCSDataSource.COMMON_PLATFORM);
        assertThat(actual.getHearingType()).isEqualTo("sentenced");
        assertThat(actual.getHearingEventType()).isEqualTo("Resulted");

        assertThat(actual.getHearingDays()).asList().containsExactly(CCSHearingDay.builder()
                .courtCode("B10JQ")
                .courtRoom("ROOM 1")
                .sessionStartTime(LocalDateTime.of(2021, 8, 26, 9, 0))
                .listNo("1")
                .build());
        final var firstDefendant = courtCase.getDefendants().get(0);
        final var actualFirstDefendant = actual.getDefendants().get(0);

        assertThat(actualFirstDefendant.getDefendantId()).isNotBlank().isEqualTo(firstDefendant.getDefendantId());
        assertThat(actualFirstDefendant.getName()).isEqualTo(CCSName.builder()
                .title("title")
                .forename1("forename 1")
                .forename2("forename 2")
                .forename3("forename 3")
                .surname("surname")
                .build());
        assertThat(actualFirstDefendant.getDateOfBirth()).isEqualTo(LocalDate.of(1986, 11, 28));
        assertThat(actualFirstDefendant.getAddress()).isEqualTo(CCSAddress.builder()
                .line1("line1")
                .line2("line2")
                .line3("line3")
                .line4("line4")
                .line5("line5")
                .postcode("S1 1AB")
                .build());
        assertThat(actualFirstDefendant.getType()).isEqualTo(CCSDefendantType.ORGANISATION);
        assertThat(actualFirstDefendant.getProbationStatus()).isEqualTo("Current");
        assertThat(actualFirstDefendant.getOffences()).isEqualTo(Collections.singletonList(CCSOffence.builder()
                .offenceTitle("offence title")
                .offenceSummary("offence summary")
                .act("offence act")
                .sequenceNumber(1)
                .offenceCode("ABC001")
                .plea(CCSPlea.builder().pleaValue("value 1").pleaDate(LocalDate.now()).build())
                .verdict(CCSVerdict.builder().verdictType(CCSVerdictType.builder().description("description 1").build()).verdictDate(LocalDate.now()).build())
                .judicialResults(Collections.singletonList(CCSJudicialResult.builder()
                        .isConvictedResult(true)
                        .label("Adjournment")
                        .judicialResultTypeId("judicialResultTypeId")
                        .resultText("resultText")
                        .build()))
                .build()));

        assertThat(actualFirstDefendant.getCrn()).isEqualTo(firstDefendant.getCrn());
        assertThat(actualFirstDefendant.getCro()).isEqualTo(firstDefendant.getCro());
        assertThat(actualFirstDefendant.getPnc()).isEqualTo(firstDefendant.getPnc());
        assertThat(actualFirstDefendant.getPreSentenceActivity()).isEqualTo(firstDefendant.getPreSentenceActivity());
        assertThat(actualFirstDefendant.getPreviouslyKnownTerminationDate()).isEqualTo(firstDefendant.getPreviouslyKnownTerminationDate());
        assertThat(actualFirstDefendant.getSex()).isEqualTo(firstDefendant.getSex());
        assertThat(actualFirstDefendant.getSuspendedSentenceOrder()).isEqualTo(firstDefendant.getSuspendedSentenceOrder());
        assertThat(actualFirstDefendant.getAwaitingPsr()).isEqualTo(firstDefendant.getAwaitingPsr());
        assertThat(actualFirstDefendant.getBreach()).isEqualTo(firstDefendant.getBreach());
        assertThat(actualFirstDefendant.getPersonId()).isNotBlank().isEqualTo(firstDefendant.getPersonId());
    }

    @Test
    public void shouldMapMultipleDefendants() {
        final var aCase = DomainDataHelper.aHearingWithAllFields();
        final var courtCase = aCase
                .withCaseId(null)
                .withDefendants(List.of(
                        aCase.getDefendants().get(0)
                                .withDefendantId("1234")
                                .withPersonId("person id 1"),
                        aCase.getDefendants().get(0)
                                .withDefendantId("5678")
                                .withPersonId("person id 2")
                ));

        final var actual = CCSExtendedHearing.of(courtCase);

        assertThat(actual.getDefendants().get(0).getDefendantId()).isEqualTo("1234");
        assertThat(actual.getDefendants().get(0).getPersonId()).isEqualTo("person id 1");
        assertThat(actual.getDefendants().get(1).getDefendantId()).isEqualTo("5678");
        assertThat(actual.getDefendants().get(1).getPersonId()).isEqualTo("person id 2");
    }

    @Test
    public void shouldMapBack() {
        final var aDefendant = DomainDataHelper.aHearingWithAllFields()
                .getDefendants().get(0)
                // confirmedOffender should not be mapped back
                .withConfirmedOffender(null);
        final var original = DomainDataHelper.aHearingWithAllFields()
                .withDefendants(List.of(aDefendant));

        final var actual = CCSExtendedHearing.of(original)
                .asDomain();

        assertThat(actual).isEqualTo(original);
    }
}
