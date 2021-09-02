package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CCSCourtCase implements Serializable {

    private final String caseId;
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
    private final CCSName name;
    private final String defendantName;
    private final CCSAddress defendantAddress;
    private final LocalDate defendantDob;
    private final CCSDefendantType defendantType;
    private final String defendantSex;
    private final String listNo;
    private final String nationality1;
    private final String nationality2;
    private final Boolean breach;
    private final LocalDate previouslyKnownTerminationDate;
    private final Boolean suspendedSentenceOrder;
    private final boolean preSentenceActivity;
    private final boolean awaitingPsr;

    @JsonIgnore
    private final CCSGroupedOffenderMatchesRequest groupedOffenderMatches;

    @JsonIgnore
    private final boolean isNew;

    public boolean isPerson() {
        return Optional.ofNullable(defendantType).map(defType -> defType == CCSDefendantType.PERSON).orElse(false);
    }

    public static CCSCourtCase of(CourtCase domain){
        return CCSCourtCase.builder()
                .defendantId(domain.getDefendantId())
                .awaitingPsr(domain.isAwaitingPsr())
                .breach(domain.getBreach())
                .caseId(domain.getCaseId())
                .caseNo(domain.getCaseNo())
                .courtCode(domain.getCourtCode())
                .courtRoom(domain.getCourtRoom())
                .crn(domain.getCrn())
                .cro(domain.getCro())
                .pnc(domain.getPnc())
                .preSentenceActivity(domain.isPreSentenceActivity())
                .previouslyKnownTerminationDate(domain.getPreviouslyKnownTerminationDate())
                .probationStatusActual(domain.getProbationStatus())
                .sessionStartTime(domain.getSessionStartTime())
                .suspendedSentenceOrder(domain.getSuspendedSentenceOrder())
                .defendantDob(domain.getDefendantDob())
                .defendantName(CaseMapper.nameFrom(domain.getDefendantName(), domain.getName()))
                .defendantType(CCSDefendantType.of(domain.getDefendantType()))
                .defendantSex(domain.getDefendantSex())
                .isNew(domain.isNew())
                .listNo(domain.getListNo())
                .nationality1(domain.getNationality1())
                .nationality2(domain.getNationality2())

                .name(Optional.ofNullable(domain.getName())
                        .map(CCSName::of)
                        .orElse(null))
                .defendantAddress(Optional.ofNullable(domain.getDefendantAddress())
                        .map(CCSAddress::of)
                        .orElse(null))
                .offences(Optional.ofNullable(domain.getOffences())
                        .map(offences -> offences.stream()
                                .map(CCSOffence::of)
                                .collect(Collectors.toList()))
                        .orElse(null))
                .build();
    }

    public CourtCase asDomain() {
        return CourtCase.builder()
                .caseId(Optional.ofNullable(getCaseId()).orElse(UUID.randomUUID().toString()))
                .defendantId(Optional.ofNullable(getDefendantId()).orElse(UUID.randomUUID().toString()))
                .awaitingPsr(isAwaitingPsr())
                .breach(getBreach())
                .caseNo(getCaseNo())
                .courtCode(getCourtCode())
                .courtRoom(getCourtRoom())
                .crn(getCrn())
                .cro(getCro())
                .pnc(getPnc())
                .preSentenceActivity(isPreSentenceActivity())
                .previouslyKnownTerminationDate(getPreviouslyKnownTerminationDate())
                .probationStatus(getProbationStatusActual())
                .sessionStartTime(getSessionStartTime())
                .suspendedSentenceOrder(getSuspendedSentenceOrder())
                .defendantDob(getDefendantDob())
                .defendantName(getDefendantName())
                .defendantType(getDefendantType().asDomain())
                .defendantSex(getDefendantSex())
                .isNew(isNew())
                .listNo(getListNo())
                .nationality1(getNationality1())
                .nationality2(getNationality2())

                .name(Optional.ofNullable(getName())
                        .map(CCSName::asDomain)
                        .orElse(null))
                .defendantAddress(Optional.ofNullable(getDefendantAddress())
                        .map(CCSAddress::asDomain)
                        .orElse(null))
                .offences(Optional.ofNullable(getOffences())
                        .map(offences -> offences.stream()
                                .map(CCSOffence::asDomain)
                                .collect(Collectors.toList()))
                        .orElse(null))

                .build();
    }
}
