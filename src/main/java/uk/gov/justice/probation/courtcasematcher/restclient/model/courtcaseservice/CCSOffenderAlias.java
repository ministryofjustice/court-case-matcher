package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OffenderAlias;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CCSOffenderAlias {

    private final LocalDate dateOfBirth;
    private final String firstName;
    private final List<String> middleNames;
    private final String surname;
    private final String gender;

    public static CCSOffenderAlias of(final OffenderAlias offenderAlias) {
        return CCSOffenderAlias.builder()
                .firstName(offenderAlias.getFirstName())
                .middleNames(offenderAlias.getMiddleNames())
                .surname(offenderAlias.getSurname())
                .dateOfBirth(offenderAlias.getDateOfBirth())
                .gender(offenderAlias.getGender())
                .build();
    }
}
