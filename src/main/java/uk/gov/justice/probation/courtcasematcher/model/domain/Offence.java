package uk.gov.justice.probation.courtcasematcher.model.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Offence {
    private final String id;

    private final String offenceTitle;

    private final String offenceSummary;

    private final String act;

    private final Integer sequenceNumber;

    private final Integer listNo;

    private List<JudicialResult> judicialResults;
}
