package uk.gov.justice.probation.courtcasematcher.restclient;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personrecordservice.Person;
import uk.gov.justice.probation.courtcasematcher.restclient.model.personrecordservice.PersonSearchRequest;

import java.util.List;

@Component
@Slf4j
public class PersonRecordServiceClient {

    @Setter
    @Value("${person-record-service.person-search-url}")
    private String personSearchUrl;

    @Value("${person-record-service.create-person-url}")
    private String createPersonUrl;

    private final WebClient webClient;

    @Autowired
    public PersonRecordServiceClient(@Qualifier("personRecordServiceWebClient") WebClient webClient) {
        super();
        this.webClient = webClient;
    }

    public Mono<List<Person>> search(PersonSearchRequest personSearchRequest) {
        return post(personSearchUrl)
                .bodyValue(personSearchRequest)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Person>>(){})
                .onErrorResume(throwable -> {
                    log.error("Person search with {} returned with error {}", personSearchRequest, throwable.getMessage());
                    return Mono.error(throwable);

                });
    }

    public Mono<Person> createPerson(Person person) {
        return post(createPersonUrl)
                .bodyValue(person)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Person.class)
                .onErrorResume(throwable -> {
                    log.error("Create person with {} returned with error {}", person, throwable.getMessage());
                    return Mono.error(throwable);

                });
    }

    private WebClient.RequestBodySpec post(String uri) {
        return webClient
                .post()
                .uri(uri);

    }

}
