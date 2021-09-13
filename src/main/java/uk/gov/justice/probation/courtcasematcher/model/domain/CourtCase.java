package uk.gov.justice.probation.courtcasematcher.model.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import reactor.util.StringUtils;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CourtCase implements Serializable {

    private final String caseId;
    private List<Defendant> defendants;
    private List<HearingDay> hearingDays;
    @Setter(AccessLevel.NONE)
    private final String caseNo;

    // TODO: These fields should be migrated to Defendant
    private final String defendantId;
    private final String probationStatus;
    private final List<Offence> offences;
    private final String crn;
    private final String cro;
    private final String pnc;
    private final Name name;
    private final String defendantName;
    private final Address defendantAddress;
    private final LocalDate defendantDob;
    private final DefendantType defendantType;
    private final String defendantSex;
    private final String nationality1;
    private final String nationality2;
    private final Boolean breach;
    private final LocalDate previouslyKnownTerminationDate;
    private final Boolean suspendedSentenceOrder;
    private final boolean preSentenceActivity;
    private final boolean awaitingPsr;

    @JsonIgnore
    private final GroupedOffenderMatches groupedOffenderMatches;

    @JsonIgnore
    private final boolean isNew;

    private final DataSource source;

    public boolean isPerson() {
        return Optional.ofNullable(defendantType).map(defType -> defType == DefendantType.PERSON).orElse(false);
    }

    public boolean shouldMatchToOffender() {
        return isPerson() && !StringUtils.hasText(crn);
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
