package uk.gov.justice.probation.courtcasematcher.restclient.model.personrecordservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class PersonSearchRequest {
    private String crn;
    private String pncNumber;
    private String forename;
    private String surname;
    private List<String> middleNames;
    private LocalDate dateOfBirth;

    public static PersonSearchRequest of(Defendant defendant){
        return PersonSearchRequest.builder()
                .crn(defendant.getCrn())
                .surname(builder().surname)
                .dateOfBirth(defendant.getDateOfBirth())
                .pncNumber(defendant.getPnc())
                .middleNames(Person.getMiddleNames(defendant))
                .build();
    }
}
