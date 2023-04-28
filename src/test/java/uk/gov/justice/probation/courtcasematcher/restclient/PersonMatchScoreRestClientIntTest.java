package uk.gov.justice.probation.courtcasematcher.restclient;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personmatchscore.PersonMatchScoreParameter;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personmatchscore.PersonMatchScoreRequest;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personmatchscore.PersonMatchScoreResponse;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
class PersonMatchScoreRestClientIntTest {
  @Autowired
  private PersonMatchScoreRestClient personMatchScoreRestClient;

  private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

  @RegisterExtension
  static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

  @Test
  public void givenPersonMatchScoreRequest_whenMatch_thenReturnMatchScoreResponse() {

    PersonMatchScoreRequest personMatchScoreRequest = PersonMatchScoreRequest.builder()
      .firstName(PersonMatchScoreParameter.of("Lily", "Lily"))
      .surname(PersonMatchScoreParameter.of("Robinson", "Robibnson"))
      .pnc(PersonMatchScoreParameter.of("2001/0141640Y", "None"))
      .dateOfBirth(PersonMatchScoreParameter.of("2009-07-06", "2009-07-06"))
      .sourceDataset(PersonMatchScoreParameter.of("COMMON_PLATFORM", "DELIUS"))
      .uniqueId(PersonMatchScoreParameter.of("1111", "4444"))
      .build();
      var response = personMatchScoreRestClient.match(personMatchScoreRequest).block();
      assertThat(response).isEqualTo(
        PersonMatchScoreResponse.builder()
          .matchProbability(PersonMatchScoreParameter.of(0.9172587927, null)).build());
  }

  @Test
  public void givenPersonMatchScoreRequest_whenMatchReturnsHttpError_thenReturnMonoError() {

    PersonMatchScoreRequest personMatchScoreRequest = PersonMatchScoreRequest.builder()
      .firstName(PersonMatchScoreParameter.of("Throw", "Error"))
      .surname(PersonMatchScoreParameter.of("Robinson", "Robibnson"))
      .pnc(PersonMatchScoreParameter.of("2001/0141640Y", "None"))
      .dateOfBirth(PersonMatchScoreParameter.of("2009-07-06", "2009-07-06"))
      .sourceDataset(PersonMatchScoreParameter.of("COMMON_PLATFORM", "DELIUS"))
      .uniqueId(PersonMatchScoreParameter.of("1111", "4444"))
      .build();
    Assertions.assertThrows(RuntimeException.class, () -> {
      personMatchScoreRestClient.match(personMatchScoreRequest).block();
    });
  }

}