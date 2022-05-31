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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class Defendant {
    private String defendantId;
    @EqualsAndHashCode.Include
    private Address address;
    @EqualsAndHashCode.Include
    private LocalDate dateOfBirth;
    @EqualsAndHashCode.Include
    private Name name;
    @EqualsAndHashCode.Include
    private List<Offence> offences;
    private String probationStatus;
    @EqualsAndHashCode.Include
    private DefendantType type;
    private String crn;
    @EqualsAndHashCode.Include
    private String cro;
    @EqualsAndHashCode.Include
    private String pnc;
    private Boolean preSentenceActivity;
    private LocalDate previouslyKnownTerminationDate;
    @EqualsAndHashCode.Include
    private String sex;
    private Boolean suspendedSentenceOrder;
    private Boolean awaitingPsr;
    private Boolean breach;
    @EqualsAndHashCode.Include
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
