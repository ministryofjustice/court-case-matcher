package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Verdict;

import java.time.LocalDate;
import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CCSVerdict {
    private String typeDescription;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    public static CCSVerdict of(Verdict verdict) {
        return CCSVerdict.builder()
                .typeDescription(Optional.ofNullable(verdict).map(Verdict::getTypeDescription).orElse(null))
                .date(Optional.ofNullable(verdict).map(Verdict::getDate).orElse(null))
                .build();
    }

    public Verdict asDomain() {
        return Verdict.builder()
                .typeDescription(typeDescription)
                .date(date)
                .build();
    }
}
