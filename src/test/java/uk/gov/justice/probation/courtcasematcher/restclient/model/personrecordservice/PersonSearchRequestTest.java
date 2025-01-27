package uk.gov.justice.probation.courtcasematcher.restclient.model.personrecordservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PersonSearchRequestTest {

    Defendant defendant;

    @BeforeEach
    void setUp() {
        defendant = Defendant.builder()
                .name(Name.builder()
                        .forename1("forename1")
                        .forename2("forename2")
                        .forename3("forename3")
                        .surname("surname")
                        .build())
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .crn("CRN1234")
                .pnc("PNC1234")
                .build();
    }

    @Test
    public void shouldCreate_personSearchRequest_givenDefendant() {
        //when
        PersonSearchRequest personSearchRequest = PersonSearchRequest.of(defendant);
        //then
        assertThat(personSearchRequest.getForenameOne()).isEqualTo("forename1");
        assertThat(personSearchRequest.getForenameTwo()).isEqualTo("forename2");
        assertThat(personSearchRequest.getForenameThree()).isEqualTo("forename3");
        assertThat(personSearchRequest.getSurname()).isEqualTo("surname");
        assertThat(personSearchRequest.getPncNumber()).isEqualTo("PNC1234");
        assertThat(personSearchRequest.getCrn()).isEqualTo("CRN1234");
    }

    @Test
    public void shouldCreate_emptyPersonSearchRequest_givenEmptyDefendant() {
        //when
        PersonSearchRequest personSearchRequest = PersonSearchRequest.of(Defendant.builder().build());
        //then
        assertThat(personSearchRequest)
                .hasAllNullFieldsOrProperties();
    }


}