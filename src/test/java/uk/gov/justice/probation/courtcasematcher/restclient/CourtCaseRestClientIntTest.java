package uk.gov.justice.probation.courtcasematcher.restclient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.probation.courtcasematcher.pact.DomainDataHelper.CASE_ID;
import static uk.gov.justice.probation.courtcasematcher.pact.DomainDataHelper.aCourtCaseBuilderWithAllFields;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
@ExtendWith(MockitoExtension.class)
public class CourtCaseRestClientIntTest {
    public static final String CASE_ID_SERVER_ERROR = "771F1C21-D2CA-4235-8659-5C3C7D7C58B6";
    @Mock
    private Mono<Void> mono;
    @Mock
    private Mono<CourtCase> courtCaseMono;
    @MockBean
    private LegacyCourtCaseRestClient legacyClient;

    @Autowired
    @Qualifier("courtCaseServiceWebClient")
    private WebClient webClient;

    @Autowired
    private CourtCaseRestClient client;

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @Test
    public void whenPutOk_thenItsSuccessful() {
        final var courtCase = aCourtCaseBuilderWithAllFields()
                .build();
        final var voidMono = client.putCourtCase(courtCase);
        assertThat(voidMono.blockOptional()).isEmpty();

        MOCK_SERVER.findAllUnmatchedRequests();
        MOCK_SERVER.verify(
                putRequestedFor(urlEqualTo(String.format("/case/%s/extended", CASE_ID)))
        );
    }



    @Test
    void whenRestClientThrows500OnPut_ThenThrow() {
        final var aCase = aCourtCaseBuilderWithAllFields()
                .caseId(CASE_ID_SERVER_ERROR)
                .courtCode("X500")
                .build();

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> client.putCourtCase(aCase).block())
                .withMessage("Retries exhausted: 1/1");
    }

    @Test
    public void postMatches_delegatesToLegacyClient() {
        final var offenderMatches = GroupedOffenderMatches.builder().build();
        when(legacyClient.postMatches("court code", "case no", offenderMatches)).thenReturn(mono);

        final var actualMono = client.postMatches("court code", "case no", offenderMatches);

        assertThat(actualMono).isEqualTo(mono);
    }

    @Test
    public void getCourtCase_delegatesToLegacyClient() {
        when(legacyClient.getCourtCase("court code", "case no")).thenReturn(courtCaseMono);

        final var actualMono = client.getCourtCase("court code", "case no");

        assertThat(actualMono).isEqualTo(courtCaseMono);
    }
}
