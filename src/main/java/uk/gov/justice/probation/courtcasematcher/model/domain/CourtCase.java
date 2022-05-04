package uk.gov.justice.probation.courtcasematcher.model.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CourtCase implements Serializable {

    private final String caseId;
    private final String hearingId;

    @Setter(AccessLevel.NONE)
    private final String caseNo;
    private final String urn;
    private List<Defendant> defendants;
    private List<HearingDay> hearingDays;

    private final DataSource source;

    public boolean shouldMatchToOffender() {
        return defendants.stream()
                .anyMatch(Defendant::shouldMatchToOffender);
    }

    public LocalDate getDateOfHearing() {
        return getFirstHearingDay()
                .map(hearingDay -> hearingDay.getSessionStartTime().toLocalDate())
                .orElse(null);
    }

    public String getCourtCode() {
        return getFirstHearingDay()
                .map(HearingDay::getCourtCode)
                .orElseThrow();
    }


    /**
     @deprecated This method is used as a shim for simplifying the process of refactoring to introduce multiple
     hearing days. It will be removed as soon as refactoring is complete to handle these cases.
     */
    @Deprecated(forRemoval = true)
    public Optional<HearingDay> getFirstHearingDay() {
        return Optional.ofNullable(hearingDays)
                .flatMap(days -> days.stream().findFirst());
    }

    public String getCourtRoom() {
        return getFirstHearingDay()
                .map(HearingDay::getCourtRoom)
                .orElse(null);
    }

    public LocalDateTime getSessionStartTime() {
        return getFirstHearingDay()
                .map(HearingDay::getSessionStartTime)
                .orElse(null);
    }

    public String getListNo() {
        return getFirstHearingDay()
                .map(HearingDay::getListNo)
                .orElse(null);
    }
}
