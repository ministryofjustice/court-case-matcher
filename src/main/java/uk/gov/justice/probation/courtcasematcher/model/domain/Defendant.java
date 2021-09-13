package uk.gov.justice.probation.courtcasematcher.model.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
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
}
