package uk.gov.justice.probation.courtcasematcher.restclient.model.personrecordservice;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class Person {
    private UUID personId;
    private String crn;
    private String pncNumber;
    private String givenName;
    private String familyName;
    private List<String> middleNames;
    private LocalDate dateOfBirth;
    private OtherIdentifiers otherIdentifiers;
}
