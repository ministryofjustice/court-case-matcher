package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.JudicialResult;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;
import uk.gov.justice.probation.courtcasematcher.model.domain.Plea;
import uk.gov.justice.probation.courtcasematcher.model.domain.Verdict;
import uk.gov.justice.probation.courtcasematcher.model.domain.VerdictType;

import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class CCSOffenceTest {
    @Test
    public void mapOffenceRequest() {
        final var offence = buildOffence();
        final var ccsOffence = CCSOffence.of(offence);

        assertThat(ccsOffence).usingRecursiveComparison().isEqualTo(offence);

    }

    @Test
    public void mapBackToDomain() {
        final var original = buildOffence();
        final var actual = CCSOffence.of(original).asDomain();

        assertThat(actual).isEqualTo(original);
    }

    private Offence buildOffence() {
        return Offence.builder()
                .offenceTitle("title")
                .offenceSummary("summary")
                .act("act")
                .sequenceNumber(1)
                .listNo(30)
                .plea(Plea.builder()
                        .pleaValue("value")
                        .pleaDate(LocalDate.now())
                        .build())
                .verdict(Verdict.builder()
                        .verdictType(VerdictType.builder().description("description").build())
                        .verdictDate(LocalDate.now())
                        .build())
                .judicialResults(Collections.singletonList(JudicialResult.builder()
                                .isConvictedResult(false)
                                .label("label")
                                .judicialResultTypeId("judicialResultTypeId")
                        .build()))
                .build();
    }


}
