package uk.gov.justice.probation.courtcasematcher.model.courtcaseserviceapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourtCaseApi implements Serializable {

    private String caseId;

    @Setter(AccessLevel.NONE)
    private String caseNo;

    @Setter(AccessLevel.NONE)
    private String courtCode;

    private String courtRoom;

    private LocalDateTime sessionStartTime;

    private String probationStatus;

    private List<OffenceApi> offences;

    private String crn;

    private String pnc;

    private String defendantName;

    private AddressApi defendantAddress;

    private LocalDate defendantDob;

    private String defendantSex;

    private String listNo;

    private String nationality1;

    private String nationality2;

    private Boolean breach;

    private Boolean suspendedSentenceOrder;

}
