package uk.gov.justice.probation.courtcasematcher.messaging.model.libra;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@Getter
@EqualsAndHashCode
@ToString
public class LibraName {

    private final String title;
    private final String forename1;
    private final String forename2;
    private final String forename3;
    private final String surname;

    public String getForenames() {
        return Stream.of(forename1, forename2, forename3)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(" "))
            .trim();
    }

    public String getFullName() {
        return Stream.of(title, forename1, forename2, forename3, surname)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(" "))
            .trim();
    }

    public uk.gov.justice.probation.courtcasematcher.model.domain.Name asDomain() {
        return uk.gov.justice.probation.courtcasematcher.model.domain.Name.builder()
                .title(getTitle())
                .forename1(getForename1())
                .forename2(getForename2())
                .forename3(getForename3())
                .surname(getSurname())
                .build();
    }
}
