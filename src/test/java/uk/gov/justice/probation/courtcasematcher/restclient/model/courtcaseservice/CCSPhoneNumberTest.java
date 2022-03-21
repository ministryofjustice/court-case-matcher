package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.PhoneNumber;

import static org.assertj.core.api.Assertions.assertThat;

class CCSPhoneNumberTest {

    @Test
    void shouldReturnCCSPhoneNumberForGivenPhoneNumber() {
        var phoneNumner = PhoneNumber.builder()
                .home("07000000007")
                .work("07000000008")
                .mobile("07000000009")
                .build();
        assertThat(CCSPhoneNumber.of(phoneNumner)).isEqualTo(
                CCSPhoneNumber.builder()
                        .home(phoneNumner.getHome())
                        .work(phoneNumner.getWork())
                        .mobile(phoneNumner.getMobile())
                .build());
    }
}