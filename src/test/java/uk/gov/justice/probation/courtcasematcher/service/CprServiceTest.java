package uk.gov.justice.probation.courtcasematcher.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.Address;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.restclient.CprServiceClient;
import uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice.CprAddress;
import uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice.CprDefendant;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CprServiceTest {

    @Mock
    private CprServiceClient cprServiceClient;

    private CprService cprService;

    @BeforeEach
    public void setup() {
        cprService = new CprService(cprServiceClient);
    }

    @Test
    public void shouldGetCprCanonicalRecord() {
        Defendant defendantFromInitialPayload = Defendant.builder()
            .cprUUID("1234").build();
        CprDefendant cprDefendant = CprDefendant.builder()
            .dateOfBirth("02/02/1995")
            .sex("Male")
            .addresses(List.of(CprAddress.builder()
                .buildingNumber("31")
                .thoroughfareName("Something Road")
                .dependentLocality("Rusholme")
                .postTown("Manchester")
                .postcode("S1 3RU").build()))
            .build();

        when(cprServiceClient.getCprCanonicalRecord(anyString())).thenReturn(Mono.just(cprDefendant));
        cprService.updateDefendant(defendantFromInitialPayload);

        verify(cprServiceClient).getCprCanonicalRecord(anyString());
        assertThat(defendantFromInitialPayload.getCprUUID()).isEqualTo("1234");
        assertThat(defendantFromInitialPayload.getSex()).isEqualTo("MALE");
        assertThat(defendantFromInitialPayload.getAddress()).isEqualTo(Address.builder()
                .line1("31")
                .line2("Something Road")
                .line3("Rusholme")
                .line4("Manchester")
                .postcode("S1 3RU")
            .build());
    }

    @Test
    public void shouldNotFindCprCanonicalRecord() {
        Defendant defendantFromInitialPayload = Defendant.builder()
            .cprUUID("1234").build();
        when(cprServiceClient.getCprCanonicalRecord(anyString())).thenReturn(Mono.empty());
        cprService.updateDefendant(defendantFromInitialPayload);

        verify(cprServiceClient).getCprCanonicalRecord(anyString());
        assertThat(defendantFromInitialPayload.getCprUUID()).isEqualTo("1234");
        assertThat(defendantFromInitialPayload.getSex()).isEqualTo("NOT_KNOWN");
    }

    @Test
    public void shouldSetTheLatestDefendantAddress() {
        Defendant defendantFromInitialPayload = Defendant.builder()
            .cprUUID("1234").build();
        CprDefendant cprDefendant = CprDefendant.builder()
            .dateOfBirth("02/02/1995")
            .sex("Male")
            .addresses(List.of(
                CprAddress.builder()
                    .buildingNumber("31")
                    .thoroughfareName("Something Road")
                    .dependentLocality("Rusholme")
                    .postTown("Manchester")
                    .postcode("S1 3RU").build(),
                CprAddress.builder()
                    .endDate("02/02/1995")
                    .buildingNumber("11")
                    .thoroughfareName("That Road")
                    .dependentLocality("Rusholme")
                    .postTown("Manchester")
                    .postcode("S1 4RU").build()))
            .build();

        when(cprServiceClient.getCprCanonicalRecord(anyString())).thenReturn(Mono.just(cprDefendant));
        cprService.updateDefendant(defendantFromInitialPayload);

        verify(cprServiceClient).getCprCanonicalRecord(anyString());
        assertThat(defendantFromInitialPayload.getCprUUID()).isEqualTo("1234");
        assertThat(defendantFromInitialPayload.getSex()).isEqualTo("MALE");
        assertThat(defendantFromInitialPayload.getAddress()).isEqualTo(Address.builder()
            .line1("31")
            .line2("Something Road")
            .line3("Rusholme")
            .line4("Manchester")
            .postcode("S1 3RU")
            .build());
    }
}
