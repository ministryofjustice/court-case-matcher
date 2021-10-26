package uk.gov.justice.probation.courtcasematcher.application;

import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.justice.probation.courtcasematcher.application.healthchecks.SqsCheck;

@TestConfiguration
public class TestMessagingConfig {

    @MockBean
    public SqsCheck sqsCheck;

    @MockBean
    private BuildProperties buildProperties;

}
