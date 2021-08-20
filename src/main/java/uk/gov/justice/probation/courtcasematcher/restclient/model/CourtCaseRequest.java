package uk.gov.justice.probation.courtcasematcher.restclient.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import reactor.util.StringUtils;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourtCaseRequest implements Serializable {

    private final String caseId;

    @Setter(AccessLevel.NONE)
    private final String caseNo;

    @Setter(AccessLevel.NONE)
    private final String courtCode;

    private final String courtRoom;

    private final LocalDateTime sessionStartTime;

    private final String probationStatus;

    private final String probationStatusActual;

    private final List<OffenceRequest> offences;

    private final String crn;

    private final String cro;

    private final String pnc;

    private final NameRequest name;

    private final String defendantName;

    private final AddressRequest defendantAddress;

    private final LocalDate defendantDob;

    private final DefendantType defendantType;

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
    private final GroupedOffenderMatchesRequest groupedOffenderMatches;

    @JsonIgnore
    private final boolean isNew;

    public boolean isPerson() {
        return Optional.ofNullable(defendantType).map(defType -> defType == DefendantType.PERSON).orElse(false);
    }

    public boolean shouldMatchToOffender() {
        return isPerson() && !StringUtils.hasText(crn);
    }


    public LocalDate getDateOfHearing() {
        return sessionStartTime != null ? sessionStartTime.toLocalDate() : null;
    }


    public static CourtCaseRequest of(CourtCase domain){
        return CourtCaseRequest.builder()
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
                .probationStatus(domain.getProbationStatus())
                .probationStatusActual(domain.getProbationStatusActual())
                .sessionStartTime(domain.getSessionStartTime())
                .suspendedSentenceOrder(domain.getSuspendedSentenceOrder())
                .defendantDob(domain.getDefendantDob())
                .defendantName(CaseMapper.nameFrom(domain.getDefendantName(), domain.getName()))
                .defendantType(DefendantType.of(domain.getDefendantType()))
                .defendantSex(domain.getDefendantSex())
                .isNew(domain.isNew())
                .listNo(domain.getListNo())
                .nationality1(domain.getNationality1())
                .nationality2(domain.getNationality2())

                .name(Optional.ofNullable(domain.getName())
                        .map(NameRequest::of)
                        .orElse(null))
                .defendantAddress(Optional.ofNullable(domain.getDefendantAddress())
                        .map(AddressRequest::of)
                        .orElse(null))
                .offences(Optional.ofNullable(domain.getOffences())
                        .map(offences -> offences.stream()
                                .map(OffenceRequest::of)
                                .collect(Collectors.toList()))
                        .orElse(null))
                .build();
    }
}
