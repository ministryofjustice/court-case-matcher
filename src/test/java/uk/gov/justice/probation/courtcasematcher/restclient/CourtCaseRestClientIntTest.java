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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.probation.courtcasematcher.pact.DomainDataHelper.aCourtCaseBuilderWithAllFields;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
@ExtendWith(MockitoExtension.class)
public class CourtCaseRestClientIntTest {
    @Mock
    private Mono<Void> mono;
    @Mock
    private Mono<CourtCase> courtCaseMono;
    @MockBean
    private LegacyCourtCaseRestClient legacyClient;

    @Autowired
    @Qualifier("courtCaseServiceWebClient")
    private WebClient webClient;

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @Autowired
    private CourtCaseRestClient client;

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @Test
    public void validPut_isSuccessful() {
        final var courtCase = aCourtCaseBuilderWithAllFields()
                .caseId("D517D32D-3C80-41E8-846E-D274DC2B94A5")
                .build();
        final var voidMono = client.putCourtCase(courtCase);
        assertThat(voidMono.blockOptional()).isEmpty();
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
