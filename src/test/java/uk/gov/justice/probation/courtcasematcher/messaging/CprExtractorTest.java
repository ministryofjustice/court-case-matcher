package uk.gov.justice.probation.courtcasematcher.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.application.FeatureFlags;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.probation.courtcasematcher.messaging.CprExtractor.BASINGSTOKE;
import static uk.gov.justice.probation.courtcasematcher.messaging.CprExtractor.HIGH_WYCOMBE;
import static uk.gov.justice.probation.courtcasematcher.messaging.CprExtractor.MILTON_KEYNES;
import static uk.gov.justice.probation.courtcasematcher.messaging.CprExtractor.NEWPORT_ISLE_OF_WIGHT;
import static uk.gov.justice.probation.courtcasematcher.messaging.CprExtractor.OXFORD;
import static uk.gov.justice.probation.courtcasematcher.messaging.CprExtractor.PORTSMOUTH;
import static uk.gov.justice.probation.courtcasematcher.messaging.CprExtractor.READING;
import static uk.gov.justice.probation.courtcasematcher.messaging.CprExtractor.SOUTHAMPTON;

@ExtendWith(MockitoExtension.class)
class CprExtractorTest {

    private CprExtractor cprExtractor;

    @Mock
    private FeatureFlags featureFlags;

    @BeforeEach
    void setUp() {
        cprExtractor = new CprExtractor(featureFlags);
    }


    @ParameterizedTest
    @ValueSource(strings = {
        BASINGSTOKE, MILTON_KEYNES, NEWPORT_ISLE_OF_WIGHT,
        OXFORD, PORTSMOUTH, READING, SOUTHAMPTON, HIGH_WYCOMBE
    })
    void whenFeatureIsOn_andCourtCodeMatches_thenProcessExtraFields(String courtCode) {
        when(featureFlags.getFlag("cpr_matcher")).thenReturn(true);
        boolean canExtractCprFields = cprExtractor.canExtractCprFields(courtCode);
        assertThat(canExtractCprFields).isTrue();
    }

    @Test
    void whenFeatureIsOn_andCourtCodeDoesNotMatch_thenDoNotProcessExtraFields() {
        when(featureFlags.getFlag("cpr_matcher")).thenReturn(true);
        boolean canExtractCprFields = cprExtractor.canExtractCprFields("B13ZR");
        assertThat(canExtractCprFields).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        BASINGSTOKE, MILTON_KEYNES, NEWPORT_ISLE_OF_WIGHT,
        OXFORD, PORTSMOUTH, READING, SOUTHAMPTON, HIGH_WYCOMBE
    })
    void whenFeatureIsOff_thenDoNotProcessExtraFields(String courtCode) {
        when(featureFlags.getFlag("cpr_matcher")).thenReturn(false);
        boolean canExtractCprFields = cprExtractor.canExtractCprFields(courtCode);
        assertThat(canExtractCprFields).isFalse();
    }
}