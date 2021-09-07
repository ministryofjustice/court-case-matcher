package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
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
}
