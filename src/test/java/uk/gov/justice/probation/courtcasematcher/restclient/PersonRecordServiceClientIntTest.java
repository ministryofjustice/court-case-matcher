package uk.gov.justice.probation.courtcasematcher.restclient;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personrecordservice.Person;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personrecordservice.PersonSearchRequest;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
@ExtendWith(MockitoExtension.class)
public class PersonRecordServiceClientIntTest {

    @Autowired
    private PersonRecordServiceClient personRecordServiceClient;

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @BeforeEach
    void setUp() {
        openMocks(this);
    }

    @Nested
    class Search {
        PersonSearchRequest searchRequest = PersonSearchRequest.builder().build();

        @Test
        public void givenSearchRequest_thenReturnASingleResult() {
            var searchResponse = personRecordServiceClient.search(searchRequest).blockOptional();

            assertThat(searchResponse.get()).hasSize(1);
            assertThat(searchResponse.get().get(0)).extracting("otherIdentifiers.crn").isEqualTo("X12345");
            assertThat(searchResponse.get().get(0)).extracting("otherIdentifiers.pncNumber").isEqualTo("PNC33333");
            assertThat(searchResponse.get().get(0)).extracting("personId").isEqualTo(UUID.fromString("e374e376-e2a3-11ed-b5ea-0242ac120002"));


            MOCK_SERVER.findAllUnmatchedRequests();
            MOCK_SERVER.verify(
                    postRequestedFor(urlEqualTo("/person/search"))
            );
        }

    }

    @Nested
    class CreatePerson {
        Person person = Person.builder().build();

        @Test
        public void givenCreatePersonRequest_thenReturnSuccessWithResult() {
            var search = personRecordServiceClient.createPerson(person).blockOptional();


            MOCK_SERVER.findAllUnmatchedRequests();
            MOCK_SERVER.verify(
                    postRequestedFor(urlEqualTo("/person"))
            );
        }
    }
}
