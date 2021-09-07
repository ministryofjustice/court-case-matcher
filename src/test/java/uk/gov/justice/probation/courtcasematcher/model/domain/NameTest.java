package uk.gov.justice.probation.courtcasematcher.model.domain;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.CCSName;

import static org.assertj.core.api.Assertions.assertThat;

class NameTest {
@Test
public void map() {
    final var name = Name.builder()
            .forename1("fore1")
            .forename2("fore2")
            .forename3("fore3")
            .surname("sur")
            .title("title")
            .build();
    final var nameRequest = CCSName.of(name);

    assertThat(nameRequest).usingRecursiveComparison().isEqualTo(name);
    }
}
