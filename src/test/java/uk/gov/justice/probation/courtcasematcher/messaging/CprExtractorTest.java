package uk.gov.justice.probation.courtcasematcher.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.application.FeatureFlags;

import static org.junit.jupiter.api.Assertions.*;

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
    void whenFeatureIsOn_thenProcessExtraFields() {

    }

    @Test
    void whenFeatureIsOff_thenDoNotProcessExtraFields() {

    }




}