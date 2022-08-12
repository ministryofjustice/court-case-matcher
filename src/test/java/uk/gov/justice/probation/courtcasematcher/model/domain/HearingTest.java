package uk.gov.justice.probation.courtcasematcher.model.domain;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.type.DefendantType;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class HearingTest {

    @Test
    void givenAnyDefendantShouldMatch_thenTrue() {
        Hearing hearing = Hearing.builder()
                .defendants(List.of(Defendant.builder()
                        .type(DefendantType.ORGANISATION)
                        .build(),
                        // This one should be matched
                        Defendant.builder()
                                .type(DefendantType.PERSON)
                                .build()
                ))
                .build();
        assertThat(hearing.shouldMatchToOffender()).isTrue();
    }

    @Test
    void givenNoDefendantsShouldMatch_thenFalse() {
        Hearing hearing = Hearing.builder()
                .defendants(List.of(Defendant.builder()
                        .type(DefendantType.ORGANISATION)
                        .build(),
                        Defendant.builder()
                                .type(DefendantType.PERSON)
                                .crn("something")
                                .build()
                ))
                .build();
        assertThat(hearing.shouldMatchToOffender()).isFalse();
    }
}
