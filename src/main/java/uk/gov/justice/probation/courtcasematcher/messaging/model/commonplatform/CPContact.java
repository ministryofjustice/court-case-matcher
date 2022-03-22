package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcasematcher.model.domain.PhoneNumber;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CPContact {
    private final String home;
    private final String mobile;
    private final String work;

    public PhoneNumber asPhoneNumber() {
        return PhoneNumber.builder()
                .home(home)
                .mobile(mobile)
                .work(work)
                .build();
    }
}
