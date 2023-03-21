package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Verdict;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
public class CPVerdict {
    private String typeDescription;
    private LocalDate date;


    public Verdict asDomain(){
        return Verdict.builder()
                .typeDescription(typeDescription)
                .date(date)
                .build();
    }
}
