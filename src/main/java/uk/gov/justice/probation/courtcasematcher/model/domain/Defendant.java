package uk.gov.justice.probation.courtcasematcher.model.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.hasText;

@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class Defendant {
    private String defendantId;
    private Address address;
    private LocalDate dateOfBirth;
    private Name name;
    private List<Offence> offences;
    private String probationStatus;
    private DefendantType type;
    private String crn;
    private String cro;
    private String pnc;
    private Boolean preSentenceActivity;
    private LocalDate previouslyKnownTerminationDate;
    private String sex;
    private Boolean suspendedSentenceOrder;
    private Boolean awaitingPsr;
    private Boolean breach;
    private PhoneNumber phoneNumber;

    @JsonIgnore
    private final GroupedOffenderMatches groupedOffenderMatches;

    public boolean shouldMatchToOffender() {
        return Optional.of(this)
                .filter(defendant -> defendant.getType() == DefendantType.PERSON)
                .filter(defendant -> !hasText(defendant.getCrn()))
                .isPresent();
    }
}
