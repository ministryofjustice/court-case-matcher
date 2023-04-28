package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.CaseMarker;
import uk.gov.justice.probation.courtcasematcher.model.domain.Plea;

import java.time.LocalDate;
import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CCSPlea {
    private String value;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    public static CCSPlea of(Plea plea) {
        return CCSPlea.builder()
                .value(Optional.ofNullable(plea).map(Plea::getValue).orElse(null))
                .date(Optional.ofNullable(plea).map(Plea::getDate).orElse(null))
                .build();
    }

    public Plea asDomain() {
        return Plea.builder()
                .value(value)
                .date(date)
                .build();
    }
}