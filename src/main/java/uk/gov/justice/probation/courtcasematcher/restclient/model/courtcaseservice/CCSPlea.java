package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Plea;

import java.time.LocalDate;
import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CCSPlea {
    private String pleaValue;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate pleaDate;

    public static CCSPlea of(Plea plea) {
        return CCSPlea.builder()
                .pleaValue(Optional.ofNullable(plea).map(Plea::getPleaValue).orElse(null))
                .pleaDate(Optional.ofNullable(plea).map(Plea::getPleaDate).orElse(null))
                .build();
    }

    public Plea asDomain() {
        return Plea.builder()
                .pleaValue(pleaValue)
                .pleaDate(pleaDate)
                .build();
    }
}
