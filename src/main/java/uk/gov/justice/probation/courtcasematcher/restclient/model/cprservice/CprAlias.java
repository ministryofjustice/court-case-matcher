package uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@EqualsAndHashCode
public class CprAlias {
    private String firstName;
    private String lastName;
    private String middleNames;
    private CprTitle title;
}
