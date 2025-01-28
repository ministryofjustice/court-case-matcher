package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import org.junit.jupiter.api.Test;

import static uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPHearingTest.applyAssertions;
import static uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPHearingTest.buildHearing;

class CPHearingEventTest {

    @Test
    void mapToDomain() {
        final var commonPlatformHearingEvent = CPHearingEvent.builder().hearing(buildHearing()).build();
        final var hearings = commonPlatformHearingEvent.asDomain(cprExtractor);

        applyAssertions(hearings.getFirst());
    }

}
