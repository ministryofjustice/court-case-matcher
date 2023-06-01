package uk.gov.justice.probation.courtcasematcher.restclient;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personrecordservice.Person;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personrecordservice.PersonSearchRequest;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import java.time.LocalDate;
import java.util.List;
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
    class PersonSearch {
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
        @Test
        public void shouldReturnACreatedPersonForProvidedPersonDetails() throws JsonProcessingException {
            // Given
            Person person = Person.builder()
                    .crn("CRN8474")
                    .dateOfBirth(LocalDate.of(1968, 8, 15))
                    .familyName("Jones")
                    .givenName("Billy")
                    .middleNames(List.of("Danny", "Alex"))
                    .build();

            // When
            Person result = personRecordServiceClient.createPerson(person).block();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPersonId()).isEqualTo(UUID.fromString("205d8379-69d3-44d5-872c-ed5a13a31aad"));
            assertThat(result.getGivenName()).isEqualTo("Stephen");
            assertThat(result.getMiddleNames()).contains("Danny", "Alex");
            assertThat(result.getFamilyName()).isEqualTo("Jones");
            assertThat(result.getDateOfBirth()).isEqualTo("1968-08-15");
            assertThat(result.getOtherIdentifiers().getCrn()).isEqualTo("CRN8474");
            assertThat(result.getOtherIdentifiers().getPncNumber()).isEqualTo("PNC1234");
        }

        @Test
        public void shouldReturnBadRequestWhenInsufficientPersonDetailsAreProvided() {
            // Given
            Person person = Person.builder()
                    .crn("CRN400")
                    .givenName("Stephen")
                    .build();

            // When
            Mono<Person> result = personRecordServiceClient.createPerson(person);

            // Then
            StepVerifier.create(result)
                    .expectError(WebClientResponseException.BadRequest.class)
                    .verify();
        }
    }
}
