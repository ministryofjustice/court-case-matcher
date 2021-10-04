package uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.model.domain.MatchType;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OffenderSearchMatchTypeTest {

    @Test
    @DisplayName("Direct equivalent match type")
    void givenSameMatchType_whenMatch_ThenReturn() {
        assertThat(OffenderSearchMatchType.NAME.asDomain(true)).isSameAs(MatchType.NAME);
        assertThat(OffenderSearchMatchType.PARTIAL_NAME.asDomain(true)).isSameAs(MatchType.PARTIAL_NAME);
        assertThat(OffenderSearchMatchType.PARTIAL_NAME_DOB_LENIENT.asDomain(true)).isSameAs(MatchType.PARTIAL_NAME_DOB_LENIENT);
        assertThat(OffenderSearchMatchType.NOTHING.asDomain(true)).isSameAs(MatchType.NOTHING);
        assertThat(OffenderSearchMatchType.EXTERNAL_KEY.asDomain(true)).isSameAs(MatchType.EXTERNAL_KEY);
    }

    @Test
    @DisplayName("Mapping all supplied variations to their equivalent")
    void toUpperCase_ShouldGenerateTheExpectedUppercaseValue() {
        assertThat(OffenderSearchMatchType.ALL_SUPPLIED.asDomain(false)).isSameAs(MatchType.NAME_DOB);
        assertThat(OffenderSearchMatchType.ALL_SUPPLIED.asDomain(true)).isSameAs(MatchType.NAME_DOB_PNC);
        assertThat(OffenderSearchMatchType.ALL_SUPPLIED_ALIAS.asDomain(true)).isSameAs(MatchType.NAME_DOB_ALIAS);
    }
}
