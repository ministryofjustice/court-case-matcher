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
import uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice.CprIdentifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
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

        when(cprServiceClient.getCprCanonicalRecord(anyString())).thenReturn(Mono.just(getCprDefendant()));
        cprService.updateDefendant(defendantFromInitialPayload);

        verify(cprServiceClient).getCprCanonicalRecord(anyString());
        assertThat(defendantFromInitialPayload.getCprUUID()).isEqualTo("1234");
        assertThat(defendantFromInitialPayload.getSex()).isEqualTo("MALE");
        assertThat(defendantFromInitialPayload.getAddress()).isEqualTo(Address.builder()
                .line1("A building")
                .line2("31")
                .line3("Something Road")
                .line4("Rusholme")
                .line5("Manchester")
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
            .dateOfBirth("1995-02-02")
            .sex("Male")
            .addresses(List.of(
                CprAddress.builder()
                    .buildingName("A building")
                    .buildingNumber("31")
                    .thoroughfareName("Something Road")
                    .dependentLocality("Rusholme")
                    .postTown("Manchester")
                    .postcode("S1 3RU").build(),
                CprAddress.builder()
                    .endDate("1995-02-05")
                    .buildingNumber("11")
                    .thoroughfareName("That Road")
                    .dependentLocality("Rusholme")
                    .postTown("Manchester")
                    .postcode("S1 4RU").build()))
            .identifiers(CprIdentifier.builder().crns(List.of("1234567")).build())
            .build();

        when(cprServiceClient.getCprCanonicalRecord(anyString())).thenReturn(Mono.just(cprDefendant));
        cprService.updateDefendant(defendantFromInitialPayload);

        verify(cprServiceClient).getCprCanonicalRecord(anyString());
        assertThat(defendantFromInitialPayload.getCprUUID()).isEqualTo("1234");
        assertThat(defendantFromInitialPayload.getSex()).isEqualTo("MALE");
        assertThat(defendantFromInitialPayload.getAddress()).isEqualTo(Address.builder()
            .line1("A building")
            .line2("31")
            .line3("Something Road")
            .line4("Rusholme")
            .line5("Manchester")
            .postcode("S1 3RU")
            .build());
    }

    @Test
    public void shouldBuildGroupedOffenderMatchWithCRNs() {
        Defendant defendantFromInitialPayload = Defendant.builder()
            .cprUUID("1234").build();

        when(cprServiceClient.getCprCanonicalRecord(anyString())).thenReturn(Mono.just(getCprDefendant()));
        cprService.updateDefendant(defendantFromInitialPayload);

        verify(cprServiceClient).getCprCanonicalRecord(anyString());
        assertThat(defendantFromInitialPayload.getGroupedOffenderMatches()
            .getMatches().getFirst().getMatchIdentifiers().getCrn()).isEqualTo("1234567");
        assertThat(defendantFromInitialPayload.getGroupedOffenderMatches()
            .getMatches().getLast().getMatchIdentifiers().getCrn()).isEqualTo("98765423");
    }

    @Test
    public void shouldOnlyProcessDefendantsWithCprUUIDs() {
        Defendant defendantFromInitialPayload = Defendant.builder()
            .cprUUID("1234").build();
        Defendant defendantWithNoCprUUIDFromInitialPayload = Defendant.builder()
            .defendantId("5678").build();

        when(cprServiceClient.getCprCanonicalRecord(anyString())).thenReturn(Mono.just(getCprDefendant()));
        cprService.updateDefendants(List.of(defendantFromInitialPayload, defendantWithNoCprUUIDFromInitialPayload));

        verify(cprServiceClient, times(1)).getCprCanonicalRecord("1234");
    }

    private static CprDefendant getCprDefendant() {
        return CprDefendant.builder()
            .dateOfBirth("1995-02-02")
            .sex("Male")
            .addresses(List.of(CprAddress.builder()
                .buildingName("A building")
                .buildingNumber("31")
                .thoroughfareName("Something Road")
                .dependentLocality("Rusholme")
                .postTown("Manchester")
                .postcode("S1 3RU").build()))
            .identifiers(CprIdentifier.builder().crns(List.of("1234567", "98765423")).build())
            .build();
    }
}
