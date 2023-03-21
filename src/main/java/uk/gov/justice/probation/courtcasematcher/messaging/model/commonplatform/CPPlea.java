package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Plea;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
public class CPPlea {

    private String value;

    private LocalDate date;

    public Plea asDomain(){
        return Plea.builder()
                .value(value)
                .date(date)
                .build();
    }
}
