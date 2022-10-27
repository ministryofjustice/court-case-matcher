package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CCSLibraHearing implements Serializable {

    private final String caseId;
    private final String hearingId;
    private final String defendantId;
    @Setter(AccessLevel.NONE)
    private final String caseNo;
    @Setter(AccessLevel.NONE)
    private final String courtCode;
    private final String courtRoom;
    private final LocalDateTime sessionStartTime;
    private final String probationStatusActual;
    private final List<CCSOffence> offences;
    private final String crn;
    private final String cro;
    private final String pnc;
    private final String urn;
    private final CCSName name;
    private final String defendantName;
    private final CCSAddress defendantAddress;
    private final LocalDate defendantDob;
    private final CCSDefendantType defendantType;
    private final String defendantSex;
    private final String listNo;
    private final Boolean breach;
    private final LocalDate previouslyKnownTerminationDate;
    private final Boolean suspendedSentenceOrder;
    private final boolean preSentenceActivity;
    private final boolean awaitingPsr;
    private final String hearingEventType;
    private final CCSDataSource source;
    private final String hearingType;
    private final Boolean confirmedOffender;

    @JsonIgnore
    private final CCSGroupedOffenderMatchesRequest groupedOffenderMatches;

    public Hearing asDomain() {
        return Hearing.builder()
                .source(Optional.ofNullable(source).map(CCSDataSource::asDomain).orElse(null))
                .caseId(getCaseId())
                .hearingId(getHearingId())
                .hearingEventType(hearingEventType)
                .hearingType(hearingType)
                .caseNo(getCaseNo())
                .urn(getUrn())
                .hearingEventType(hearingEventType)

                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(getCourtCode())
                        .courtRoom(getCourtRoom())
                        .sessionStartTime(getSessionStartTime())
                        .listNo(getListNo())
                        .build()))

                .defendants(Collections.singletonList(Defendant.builder()
                        .defendantId(getDefendantId())
                        .awaitingPsr(isAwaitingPsr())
                        .breach(getBreach())
                        .crn(getCrn())
                        .cro(getCro())
                        .pnc(getPnc())
                        .preSentenceActivity(isPreSentenceActivity())
                        .previouslyKnownTerminationDate(getPreviouslyKnownTerminationDate())
                        .probationStatus(getProbationStatusActual())
                        .suspendedSentenceOrder(getSuspendedSentenceOrder())
                        .dateOfBirth(getDefendantDob())
                        .type(Optional.ofNullable(getDefendantType())
                                .map(CCSDefendantType::asDomain)
                                .orElse(null))
                        .sex(getDefendantSex())
                        .name(Optional.ofNullable(getName())
                                .map(CCSName::asDomain)
                                .orElse(null))
                        .address(Optional.ofNullable(getDefendantAddress())
                                .map(CCSAddress::asDomain)
                                .orElse(null))
                        .offences(Optional.ofNullable(getOffences())
                                .map(offences -> offences.stream()
                                        .map(CCSOffence::asDomain)
                                        .collect(Collectors.toList()))
                                .orElse(null))
                                .confirmedOffender(getConfirmedOffender())
                        .build()))

                .build();
    }
}
