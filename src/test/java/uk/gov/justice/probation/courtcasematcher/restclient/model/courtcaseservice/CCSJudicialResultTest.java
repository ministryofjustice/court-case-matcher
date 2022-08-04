package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.JudicialResult;
import uk.gov.justice.probation.courtcasematcher.model.domain.JudicialResultType;

import static org.assertj.core.api.Assertions.assertThat;

public class CCSJudicialResultTest {
    @Test
    public void mapJudicialResultRequest() {
        final var judicialResult = buildJudicialResult();
        final var ccsJudicialResult = CCSJudicialResult.of(judicialResult);

        assertThat(ccsJudicialResult).usingRecursiveComparison().isEqualTo(judicialResult);

    }

    @Test
    public void mapBackToDomain() {
        final var original = buildJudicialResult();
        final var actual = CCSJudicialResult.of(original).asDomain();

        assertThat(actual).isEqualTo(original);
    }

    private JudicialResult buildJudicialResult() {
        return JudicialResult.builder()
                .judicialResultType(JudicialResultType.builder().build())
                .build();
    }
}
