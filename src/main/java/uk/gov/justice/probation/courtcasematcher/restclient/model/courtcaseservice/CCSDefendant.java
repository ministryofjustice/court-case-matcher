package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.Sex;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CCSDefendant {
    private String defendantId;
    private CCSAddress address;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    private CCSName name;
    private List<CCSOffence> offences;
    private String probationStatus;
    private CCSDefendantType type;
    private String crn;
    private String cro;
    private String pnc;
    private Boolean preSentenceActivity;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate previouslyKnownTerminationDate;
    private String sex;
    private Boolean suspendedSentenceOrder;
    private Boolean awaitingPsr;
    private Boolean breach;
    private CCSPhoneNumber phoneNumber;

    public static CCSDefendant of(Defendant defendant) {
        return builder()
                .defendantId(defendant.getDefendantId())
                .name(CCSName.of(defendant.getName()))
                .dateOfBirth(defendant.getDateOfBirth())
                .address(Optional.ofNullable(defendant.getAddress()).map(CCSAddress::of).orElse(null))
                .type(CCSDefendantType.of(defendant.getType()))
                .probationStatus(defendant.getProbationStatus())
                .offences(Optional.of(defendant)
                        .map(Defendant::getOffences)
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(CCSOffence::of)
                        .collect(Collectors.toList()))
                .crn(defendant.getCrn())
                .pnc(defendant.getPnc())
                .cro(defendant.getCro())
                .preSentenceActivity(defendant.getPreSentenceActivity())
                .suspendedSentenceOrder(defendant.getSuspendedSentenceOrder())
                .sex(Sex.getNormalisedSex(defendant.getSex()).toString())
                .previouslyKnownTerminationDate(defendant.getPreviouslyKnownTerminationDate())
                .awaitingPsr(defendant.getAwaitingPsr())
                .breach(defendant.getBreach())
                .phoneNumber(Optional.ofNullable(defendant.getPhoneNumber()).map(CCSPhoneNumber::of).orElse(null))

                .build();
    }

    public Defendant asDomain() {
        return Defendant.builder()
                .defendantId(defendantId)
                .name(name.asDomain())
                .dateOfBirth(dateOfBirth)
                .address(Optional.ofNullable(address)
                        .map(CCSAddress::asDomain)
                        .orElse(null))
                .type(type.asDomain())
                .probationStatus(probationStatus)
                .offences(Optional.ofNullable(offences)
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(CCSOffence::asDomain)
                        .collect(Collectors.toList()))
                .crn(crn)
                .pnc(pnc)
                .cro(cro)
                .preSentenceActivity(preSentenceActivity)
                .suspendedSentenceOrder(suspendedSentenceOrder)
                .sex(sex)
                .previouslyKnownTerminationDate(previouslyKnownTerminationDate)
                .awaitingPsr(awaitingPsr)
                .breach(breach)
                .phoneNumber(Optional.ofNullable(phoneNumber)
                        .map(CCSPhoneNumber::asDomain)
                        .orElse(null))
                .build();
    }
}
