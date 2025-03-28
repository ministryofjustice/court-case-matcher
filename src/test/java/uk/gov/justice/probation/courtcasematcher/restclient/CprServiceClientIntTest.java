package uk.gov.justice.probation.courtcasematcher.restclient;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice.CprAddress;
import uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice.CprAlias;
import uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice.CprDefendant;
import uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice.CprIdentifier;
import uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice.CprNationality;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
public class CprServiceClientIntTest {
    private static final String CPR_UUID = "f91ef118-a51f-4874-9409-c0538b4ca6fd";
    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @Autowired
    private CprServiceClient cprServiceClient;

    @Test
    public void cprCanonicalRecordRetrievedSuccessfully() {
        CprDefendant cprDefendant = cprServiceClient.getCprCanonicalRecord(CPR_UUID).block();
        assertThat(cprDefendant).isNotNull();
        assertThat(cprDefendant).isEqualTo(expectedCprDefendant());
    }

    @Test
    public void cprCanonicalRecordNotRetrieved() {
        Optional<CprDefendant> cprDefendant = cprServiceClient.getCprCanonicalRecord("12345").blockOptional();

        AssertionsForClassTypes.assertThat(cprDefendant).isEmpty();

        MOCK_SERVER.findAllUnmatchedRequests();
        MOCK_SERVER.verify(
            getRequestedFor(urlEqualTo(String.format("/person/%s", "12345")))
        );
    }

    public CprDefendant expectedCprDefendant() {
        return CprDefendant.builder()
            .cprUUID(CPR_UUID)
            .firstName("John")
            .middleNames("Morgan")
            .lastName("Doe")
            .dateOfBirth("01/01/1990")
            .title("Mr")
            .sex("Male")
            .religion("Christian")
            .ethnicity("British")
            .aliases(List.of(CprAlias.builder()
                    .firstName("Jon")
                    .lastName("do")
                    .middleNames("Morgain")
                    .title("Mr")
                .build()))
            .nationalities(List.of(CprNationality.builder().nationalityCode("UK").build()))
            .addresses(List.of(CprAddress.builder()
                    .noFixedAbode("True")
                    .startDate("02/02/2020")
                    .endDate("04/04/2023")
                    .postcode("SW1H 9AJ")
                    .subBuildingName("Sub building 2")
                    .buildingName("Main Building")
                    .buildingNumber("102")
                    .thoroughfareName("Petty France")
                    .dependentLocality("Westminster")
                    .postTown("London")
                    .county("Greater London")
                    .country("United Kingdom")
                    .uprn("100120991537")
                .build()))
            .identifiers(CprIdentifier.builder()
                .crns(List.of("B123435"))
                .prisonNumbers(List.of("A1234BC"))
                .defendantIds(List.of("46caa4e5-ae06-4226-9cb6-682cb26cf025"))
                .cids(List.of("1234567"))
                .pncs(List.of("2000/1234567A"))
                .cros(List.of("123456/00A"))
                .nationalInsuranceNumbers(List.of("QQ123456B"))
                .driverLicenseNumbers(List.of("SMITH840325J912"))
                .arrestSummonsNumbers(List.of("0700000000000002536Y"))
                .build())
            .build();
    }
}