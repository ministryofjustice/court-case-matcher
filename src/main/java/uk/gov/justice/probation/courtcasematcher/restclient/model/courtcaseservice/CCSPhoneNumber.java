package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.PhoneNumber;

import java.util.Optional;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CCSPhoneNumber {

    private final String home;
    private final String mobile;
    private final String work;

    public static CCSPhoneNumber of(final PhoneNumber phoneNumber) {
        return Optional.ofNullable(phoneNumber).map(p -> builder()
                    .mobile(p.getMobile())
                    .work(p.getWork())
                    .home(p.getHome())
                    .build())
                .orElse(null);
    }

    public PhoneNumber asDomain(){
        return PhoneNumber.builder()
                .home(home)
                .mobile(mobile)
                .work(work)
                .build();
    }

}
