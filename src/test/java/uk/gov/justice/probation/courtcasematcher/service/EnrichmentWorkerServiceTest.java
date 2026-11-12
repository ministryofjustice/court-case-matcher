package uk.gov.justice.probation.courtcasematcher.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;
import uk.gov.justice.probation.courtcasematcher.restclient.EnrichmentWorkerClient;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse.EnrichedAddress;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse.EnrichedAlias;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse.EnrichedDefendant;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse.EnrichedGroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse.EnrichedMatchIdentifiers;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse.EnrichedMatchType;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse.EnrichedName;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse.EnrichedOffender;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse.EnrichedOffenderMatch;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse.EnrichedProbation;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse.EnrichmentSourceType;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse.EnrichmentStatus;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrichmentWorkerServiceTest {
    @Mock
    private EnrichmentWorkerClient enrichmentWorkerClient;

    private EnrichmentWorkerService enrichmentWorkerService;

    @BeforeEach
    void setUp() {
        enrichmentWorkerService = new EnrichmentWorkerService(enrichmentWorkerClient);
    }

    @Test
    void enrichesCommonPlatformDefendant() {
        var defendant = Defendant.builder().defendantId("cp-id").build();
        when(enrichmentWorkerClient.getCommonPlatformEnrichment("cp-id"))
                .thenReturn(Mono.just(enrichedResult(EnrichmentSourceType.COMMON_PLATFORM, "cp-id")));

        enrichmentWorkerService.updateDefendants(List.of(defendant), DataSource.COMMON_PLATFORM);

        verify(enrichmentWorkerClient).getCommonPlatformEnrichment("cp-id");
        assertThat(defendant.getCprUUID()).isEqualTo("cpr-id");
        assertThat(defendant.getName()).isEqualTo(Name.builder()
                .title("Mx").forename1("Alex").forename2("Morgan").surname("Smith").build());
        assertThat(defendant.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 2));
        assertThat(defendant.getAddress().getPostcode()).isEqualTo("SW1A 1AA");
        assertThat(defendant.getCrn()).isEqualTo("X123");
        assertThat(defendant.getOffender().getPnc()).isEqualTo("pnc");
        assertThat(defendant.getProbationStatus()).isEqualTo("CURRENT");
        assertThat(defendant.getBreach()).isTrue();
        assertThat(defendant.getGroupedOffenderMatches().getMatches()).hasSize(1);
        assertThat(defendant.getGroupedOffenderMatches().getMatches().getFirst().getMatchIdentifiers()
                .getAliases().getFirst().getMiddleNames()).containsExactly("M");
    }

    @Test
    void enrichesLibraDefendantByCid() {
        var defendant = Defendant.builder().cId("libra-id").build();
        when(enrichmentWorkerClient.getLibraEnrichment("libra-id"))
                .thenReturn(Mono.just(enrichedResult(EnrichmentSourceType.LIBRA, "libra-id")));

        enrichmentWorkerService.updateDefendants(List.of(defendant), DataSource.LIBRA);

        verify(enrichmentWorkerClient).getLibraEnrichment("libra-id");
        assertThat(defendant.getCprUUID()).isEqualTo("cpr-id");
    }

    @Test
    void leavesDefendantUnchangedWhenNoCprMatchExists() {
        var defendant = Defendant.builder().defendantId("cp-id").crn("existing-crn").build();
        when(enrichmentWorkerClient.getCommonPlatformEnrichment("cp-id")).thenReturn(Mono.just(
                new EnrichmentWorkerResponse(EnrichmentSourceType.COMMON_PLATFORM, "cp-id", EnrichmentStatus.NO_MATCH,
                        null, null, null, null, List.of(), List.of())));

        enrichmentWorkerService.updateDefendants(List.of(defendant), DataSource.COMMON_PLATFORM);

        assertThat(defendant.getCrn()).isEqualTo("existing-crn");
        assertThat(defendant.getCprUUID()).isNull();
    }

    @Test
    void rejectsFailedResultsInsteadOfApplyingThem() {
        var defendant = Defendant.builder().defendantId("cp-id").build();
        when(enrichmentWorkerClient.getCommonPlatformEnrichment("cp-id")).thenReturn(Mono.just(
                new EnrichmentWorkerResponse(EnrichmentSourceType.COMMON_PLATFORM, "cp-id", EnrichmentStatus.FAILED,
                        null, null, null, null, List.of(), List.of("CPR failed"))));

        assertThrows(IllegalStateException.class,
                () -> enrichmentWorkerService.updateDefendants(List.of(defendant), DataSource.COMMON_PLATFORM));
        assertThat(defendant.getCprUUID()).isNull();
    }

    private EnrichmentWorkerResponse enrichedResult(EnrichmentSourceType sourceType, String targetId) {
        return new EnrichmentWorkerResponse(
                sourceType,
                targetId,
                EnrichmentStatus.ENRICHED,
                new EnrichedDefendant("cpr-id",
                        new EnrichedName("Mx", "Alex", "Morgan", "Smith"),
                        LocalDate.of(1990, 1, 2),
                        "Female",
                        new EnrichedAddress("Building", "1", "Road", null, "London", "SW1A 1AA")),
                new EnrichedOffender("X123", "pnc", "cro"),
                new EnrichedProbation("CURRENT", true, true, LocalDate.of(2025, 3, 4), true),
                new EnrichedGroupedOffenderMatches(List.of(new EnrichedOffenderMatch(
                        new EnrichedMatchIdentifiers("X123", "pnc", "cro",
                                List.of(new EnrichedAlias("Alex", "Smith", List.of("M")))),
                        EnrichedMatchType.NAME_DOB_ALIAS, false, false))),
                List.of(),
                List.of());
    }
}
