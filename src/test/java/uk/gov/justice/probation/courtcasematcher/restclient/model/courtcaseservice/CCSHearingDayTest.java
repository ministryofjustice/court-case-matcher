package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CCSHearingDayTest {

    @Test
    public void map() {
        final var sessionStartTime = LocalDateTime.of(2021, 9, 13, 0, 0);
        final var hearingDay = CCSHearingDay.of(HearingDay.builder()
                .sessionStartTime(sessionStartTime)
                .listNo("1")
                .courtRoom("Room 1")
                .courtCode("B10JQ")
                .build());

        assertThat(hearingDay.getSessionStartTime()).isEqualTo(sessionStartTime);
        assertThat(hearingDay.getListNo()).isEqualTo("1");
        assertThat(hearingDay.getCourtCode()).isEqualTo("B10JQ");
        assertThat(hearingDay.getCourtRoom()).isEqualTo("Room 1");
    }

}
