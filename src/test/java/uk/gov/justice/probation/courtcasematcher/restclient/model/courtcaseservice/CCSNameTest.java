package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;

import static org.assertj.core.api.Assertions.assertThat;

class CCSNameTest {

    @Test
    public void map() {
        final var name = buildName();
        final var ccsName = CCSName.of(name);

        assertThat(ccsName).usingRecursiveComparison().isEqualTo(name);
    }

    @Test
    public void mapBack() {
        final var original = buildName();
        final var actual = CCSName.of(original).asDomain();

        assertThat(actual).isEqualTo(original);
    }

    private Name buildName() {
        return Name.builder()
                .title("t")
                .forename1("f1")
                .forename2("f2")
                .forename3("f3")
                .surname("s")
                .build();
    }

}
