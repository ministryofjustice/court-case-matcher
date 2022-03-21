package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.PhoneNumber;

import static org.assertj.core.api.Assertions.assertThat;

class CPContactTest {

    public static final CPContact TEST_CP_CONTACT = CPContact.builder().home("07000000001").work("07000000002").mobile("07000000003").build();

    @Test
    void shouldReturnPhoneNumberOnAsPhoneNumber() {
        assertThat(TEST_CP_CONTACT.asPhoneNumber()).isEqualTo(
                PhoneNumber.builder()
                        .work(TEST_CP_CONTACT.getWork())
                        .mobile(TEST_CP_CONTACT.getMobile())
                        .home(TEST_CP_CONTACT.getHome())
                        .build());
    }

}