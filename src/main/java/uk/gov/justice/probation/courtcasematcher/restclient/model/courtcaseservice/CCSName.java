package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@Getter
@EqualsAndHashCode
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CCSName {

    private final String title;
    private final String forename1;
    private final String forename2;
    private final String forename3;
    private final String surname;

    public static CCSName of(uk.gov.justice.probation.courtcasematcher.model.domain.Name name) {
        return CCSName.builder()
                .title(name.getTitle())
                .forename1(name.getForename1())
                .forename2(name.getForename2())
                .forename3(name.getForename3())
                .surname(name.getSurname())
                .build();
    }

    public Name asDomain() {
        return Name.builder()
                .title(getTitle())
                .forename1(getForename1())
                .forename2(getForename2())
                .forename3(getForename3())
                .surname(getSurname())
                .build();
    }
}
