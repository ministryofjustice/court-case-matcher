package uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode
public class CprDefendant {
    private String cprUUID;
    private String firstName;
    private String middleNames;
    private String lastName;
    private String dateOfBirth;
    private String title;
    private String masterDefendantId;
    private String sex;
    private String religion;
    private String ethnicity;
    private List<CprAlias> aliases;
    private List<CprNationality> nationalities;
    private List<CprAddress> addresses;
    private CprIdentifier identifiers;

    public Defendant asDomain() {

        return Defendant.builder().build();
    }
}
