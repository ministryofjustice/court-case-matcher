package uk.gov.justice.probation.courtcasematcher.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class FeatureFlagsTest {

    private final FeatureFlags featureFlags = new FeatureFlags();
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void getFlagReturnsTheExpectedValueForAFlag(boolean value) {
        featureFlags.setFlagValue("flag-test", value);

        assertThat(featureFlags.getFlag("flag-test")).isEqualTo(value);
    }

    @Test
    void getFlagReturnsFalseIfNotSpecified(){
        assertThat(featureFlags.getFlag("not-set")).isFalse();
    }

    @Test
    void getFlagRejectsNullParameter(){
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> featureFlags.getFlag(null));
    }
}
