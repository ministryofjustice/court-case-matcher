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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static Person from(Defendant defendant) {
        return Person.builder()
                .givenName(defendant.getName() != null ? Optional.ofNullable(defendant.getName().getForename1()).orElse(null) : null)
                .familyName(defendant.getName() != null ? Optional.ofNullable(defendant.getName().getSurname()).orElse(null) : null)
                .middleNames(getMiddleNames(defendant))
                .dateOfBirth(defendant.getDateOfBirth())
                .otherIdentifiers(OtherIdentifiers.builder()
                        .crn(Optional.ofNullable(defendant.getCrn()).orElse(null))
                        .pncNumber(Optional.ofNullable(defendant.getPnc()).orElse(null))
                        .build())
                .build();
    }

    public static List<String> getMiddleNames(Defendant defendant) {
        if (null != defendant.getName()) {
            return Stream.of(defendant.getName().getForename2(), defendant.getName().getForename3())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }
}
