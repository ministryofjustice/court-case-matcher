package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Verdict;
import uk.gov.justice.probation.courtcasematcher.model.domain.VerdictType;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
public class CPVerdict {
    private CPVerdictType verdictType;
    private LocalDate verdictDate;


    public Verdict asDomain(){
        return Verdict.builder()
                .verdictType(VerdictType.builder().description(verdictType.getDescription()).build())
                .verdictDate(verdictDate)
                .build();
    }
}
