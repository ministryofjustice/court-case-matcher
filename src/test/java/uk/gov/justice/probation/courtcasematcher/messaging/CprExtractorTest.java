package uk.gov.justice.probation.courtcasematcher.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.application.FeatureFlags;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CprExtractorTest {

    private CprExtractor cprExtractor;

    @Mock
    private FeatureFlags featureFlags;

    @BeforeEach
    void setUp() {
        cprExtractor = new CprExtractor(featureFlags);
    }

    @Test
    void whenFeatureIsOn_andCourtCodeMatches_thenProcessExtraFields() {
        when(featureFlags.getFlag("cpr_matcher")).thenReturn(true);
        boolean canExtractCprFields = cprExtractor.canExtractCprFields("TBD");
        assertThat(canExtractCprFields).isTrue();
    }

    @Test
    void whenFeatureIsOn_andCourtCodeDoesNotMatche_thenProcessExtraFields() {
        when(featureFlags.getFlag("cpr_matcher")).thenReturn(true);
        boolean canExtractCprFields = cprExtractor.canExtractCprFields("B13");
        assertThat(canExtractCprFields).isFalse();
    }

    @Test
    void whenFeatureIsOff_thenDoNotProcessExtraFields() {
        when(featureFlags.getFlag("cpr_matcher")).thenReturn(false);
        boolean canExtractCprFields = cprExtractor.canExtractCprFields("TBD");
        assertThat(canExtractCprFields).isFalse();
    }
}