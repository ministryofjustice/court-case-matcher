package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

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
public class NameRequest {

    private final String title;
    private final String forename1;
    private final String forename2;
    private final String forename3;
    private final String surname;

    public static NameRequest of(uk.gov.justice.probation.courtcasematcher.model.domain.Name name) {
        return NameRequest.builder()
                .title(name.getTitle())
                .forename1(name.getForename1())
                .forename2(name.getForename2())
                .forename3(name.getForename3())
                .surname(name.getSurname())
                .build();
    }

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

}
