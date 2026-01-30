package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPHearingTest.applyAssertions;
import static uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPHearingTest.buildHearing;

@ExtendWith(MockitoExtension.class)
class CPHearingEventTest {

    @Test
    void mapToDomain() {
        final var commonPlatformHearingEvent = CPHearingEvent.builder().hearing(buildHearing()).build();
        final var hearings = commonPlatformHearingEvent.asDomain();

        applyAssertions(hearings.getFirst());
    }

}
