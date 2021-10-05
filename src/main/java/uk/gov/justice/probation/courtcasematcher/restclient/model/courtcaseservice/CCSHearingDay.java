package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CCSHearingDay {
    private String courtCode;
    private String courtRoom;
    private String listNo;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sessionStartTime;

    public static CCSHearingDay of(HearingDay hearingDay) {
        return CCSHearingDay.builder()
                .courtRoom(hearingDay.getCourtRoom())
                .courtCode(hearingDay.getCourtCode())
                .sessionStartTime(hearingDay.getSessionStartTime())
                .listNo(hearingDay.getListNo())
                .build();
    }

    public HearingDay asDomain() {
        return HearingDay.builder()
                .courtRoom(courtRoom)
                .courtCode(courtCode)
                .sessionStartTime(sessionStartTime)
                .listNo(listNo)
                .build();
    }
}
