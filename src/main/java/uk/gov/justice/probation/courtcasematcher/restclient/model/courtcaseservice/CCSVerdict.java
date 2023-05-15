package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Verdict;
import uk.gov.justice.probation.courtcasematcher.model.domain.VerdictType;

import java.time.LocalDate;
import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CCSVerdict {
    private CCSVerdictType verdictType;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate verdictDate;

    public static CCSVerdict of(Verdict verdict) {
        return CCSVerdict.builder()
                .verdictType(CCSVerdictType.builder().description(
                        Optional.ofNullable(verdict.getVerdictType()).map(VerdictType::getDescription).orElse(null)).build())
                .verdictDate(Optional.ofNullable(verdict).map(Verdict::getVerdictDate).orElse(null))
                .build();
    }

    public Verdict asDomain() {
        return Verdict.builder()
                .verdictType(VerdictType.builder().description(Optional.ofNullable(verdictType).map(CCSVerdictType::getDescription).orElse(null)).build())
                .verdictDate(verdictDate)
                .build();
    }
}
