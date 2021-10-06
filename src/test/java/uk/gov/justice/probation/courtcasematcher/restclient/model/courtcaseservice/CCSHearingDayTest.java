package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CCSHearingDayTest {

    @Test
    public void map() {
        final var original = buildCcsHearingDay();
        final var actual = CCSHearingDay.of(original);

        assertThat(actual.getSessionStartTime()).isEqualTo(original.getSessionStartTime());
        assertThat(actual.getListNo()).isEqualTo("1");
        assertThat(actual.getCourtCode()).isEqualTo("B10JQ");
        assertThat(actual.getCourtRoom()).isEqualTo("Room 1");
    }

    @Test
    public void mapBack() {
        final var original = buildCcsHearingDay();
        final var actual = CCSHearingDay.of(original).asDomain();

        assertThat(actual).isEqualTo(original);
    }

    private HearingDay buildCcsHearingDay() {
        final var sessionStartTime = LocalDateTime.of(2021, 9, 13, 0, 0);
        return HearingDay.builder()
                .sessionStartTime(sessionStartTime)
                .listNo("1")
                .courtRoom("Room 1")
                .courtCode("B10JQ")
                .build();
    }

}
