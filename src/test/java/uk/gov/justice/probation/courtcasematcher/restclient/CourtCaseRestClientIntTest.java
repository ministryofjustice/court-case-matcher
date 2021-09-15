package uk.gov.justice.probation.courtcasematcher.restclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.application.FeatureFlags;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import java.util.Collections;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    @MockBean
    private FeatureFlags featureFlags;

    @Autowired
    private CourtCaseRestClient client;

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @BeforeEach
    public void setUp() {
        when(featureFlags.getFlags()).thenReturn(Map.of("use-legacy-court-case-rest-client", false));
    }

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
    public void whenCaseIdIsNull_thenItsSuccessful() {
        final var courtCase = aCourtCaseBuilderWithAllFields()
                .caseId(null)
                .build();
        final var voidMono = client.putCourtCase(courtCase);
        assertThat(voidMono.blockOptional()).isEmpty();

        MOCK_SERVER.findAllUnmatchedRequests();
        MOCK_SERVER.verify(
                putRequestedFor(urlMatching("/case/[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}/extended"))

        );
    }


    @Test
    void whenRestClientThrows500OnPut_ThenThrow() {
        final var aCase = aCourtCaseBuilderWithAllFields()
                .caseId(CASE_ID_SERVER_ERROR)
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("X500")
                        .build()))
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

    @Test
    public void givenFeatureEnabled_putCourtCase_delegatesToLegacyClient() {
        when(featureFlags.getFlags()).thenReturn(Map.of("use-legacy-court-case-rest-client", true));
        final var courtCase = aCourtCaseBuilderWithAllFields()
                .build();
        when(legacyClient.putCourtCase(courtCase)).thenReturn(mono);
        final var actual = client.putCourtCase(courtCase);

        verify(legacyClient).putCourtCase(courtCase);
        assertThat(actual).isEqualTo(mono);
    }

    @Test
    public void givenFeaturesNull_whenPutCourtCase_delegatesToLegacyClient() {
        when(featureFlags.getFlags()).thenReturn(null);
        final var courtCase = aCourtCaseBuilderWithAllFields()
                .build();
        when(legacyClient.putCourtCase(courtCase)).thenReturn(mono);
        final var actual = client.putCourtCase(courtCase);

        verify(legacyClient, never()).putCourtCase(courtCase);
    }
}
