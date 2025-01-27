package uk.gov.justice.probation.courtcasematcher.restclient.model.personrecordservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;

import java.time.LocalDate;
import java.util.Optional;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class PersonSearchRequest {
    private String crn;
    private String pncNumber;
    private String surname;
    private LocalDate dateOfBirth;
    private String forenameOne;
    private String forenameTwo;
    private String forenameThree;

    public static PersonSearchRequest of(Defendant defendant){
        return PersonSearchRequest.builder()
                .crn(defendant.getCrn())
                .forenameOne(Optional.ofNullable(defendant.getName()).map(Name::getForename1).orElse(null))
                .forenameTwo(Optional.ofNullable(defendant.getName()).map(Name::getForename2).orElse(null))
                .forenameThree(Optional.ofNullable(defendant.getName()).map(Name::getForename3).orElse(null))
                .surname(Optional.ofNullable(defendant.getName()).map(Name::getSurname).orElse(null))
                .dateOfBirth(defendant.getDateOfBirth())
                .pncNumber(defendant.getPnc())
                .build();
    }
}
