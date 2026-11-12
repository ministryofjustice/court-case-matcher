package uk.gov.justice.probation.courtcasematcher.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.model.domain.Address;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.model.domain.MatchIdentifiers;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offender;
import uk.gov.justice.probation.courtcasematcher.model.domain.OffenderMatch;
import uk.gov.justice.probation.courtcasematcher.model.type.MatchType;
import uk.gov.justice.probation.courtcasematcher.restclient.EnrichmentWorkerClient;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OffenderAlias;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse.EnrichedDefendant;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse.EnrichedOffenderMatch;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse.EnrichmentSourceType;
import uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker.EnrichmentWorkerResponse.EnrichmentStatus;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class EnrichmentWorkerService {
    private final EnrichmentWorkerClient enrichmentWorkerClient;

    public void updateDefendants(List<Defendant> defendants, DataSource source) {
        if (defendants == null || defendants.isEmpty()) {
            log.warn("Skipping enrichment worker: hearing has no defendants");
            return;
        }
        if (source == null) {
            throw new IllegalArgumentException("Hearing source is required for enrichment");
        }
        for (Defendant defendant : defendants) {
            String targetId = targetId(defendant, source);
            if (targetId != null) {
                updateDefendant(defendant, source, targetId);
            } else {
                log.warn("Skipping enrichment worker for defendant with no target identifier, source={}", source);
            }
        }
    }

    private void updateDefendant(Defendant defendant, DataSource source, String targetId) {
        EnrichmentSourceType expectedSource = source == DataSource.LIBRA
                ? EnrichmentSourceType.LIBRA : EnrichmentSourceType.COMMON_PLATFORM;
        log.info("Calling enrichment worker for defendant, source={}", source);
        EnrichmentWorkerResponse result = (source == DataSource.LIBRA
                ? enrichmentWorkerClient.getLibraEnrichment(targetId)
                : enrichmentWorkerClient.getCommonPlatformEnrichment(targetId)).block();

        if (result == null) {
            throw new IllegalStateException("Enrichment worker returned no result for identifier " + targetId);
        }
        if (result.sourceType() != expectedSource || !targetId.equals(result.targetId())) {
            throw new IllegalStateException("Enrichment worker returned a result for a different target");
        }
        if (result.status() == EnrichmentStatus.NO_MATCH) {
            log.info("Enrichment worker completed for defendant, source={} status={}", source, result.status());
            return;
        }
        if (result.status() != EnrichmentStatus.ENRICHED && result.status() != EnrichmentStatus.NEEDS_REVIEW) {
            throw new IllegalStateException("Enrichment worker returned unsupported status " + result.status());
        }
        if (result.defendant() == null) {
            throw new IllegalStateException("Enrichment worker returned no defendant details for " + targetId);
        }

        applyDefendantDetails(defendant, result.defendant());
        Optional.ofNullable(result.offender()).ifPresent(offender -> {
            defendant.setCrn(offender.crn());
            defendant.setPnc(offender.pnc());
            defendant.setCro(offender.cro());
            defendant.setOffender(offender.pnc() != null || offender.cro() != null
                    ? Offender.builder().pnc(offender.pnc()).cro(offender.cro()).build()
                    : null);
        });
        Optional.ofNullable(result.probation()).ifPresent(probation -> {
            defendant.setProbationStatus(probation.probationStatus());
            defendant.setAwaitingPsr(Boolean.TRUE.equals(probation.awaitingPsr()));
            defendant.setBreach(probation.breach());
            defendant.setPreviouslyKnownTerminationDate(probation.previouslyKnownTerminationDate());
            defendant.setPreSentenceActivity(Boolean.TRUE.equals(probation.preSentenceActivity()));
        });
        defendant.setGroupedOffenderMatches(mapGroupedMatches(result));
        log.info("Enrichment worker completed for defendant, source={} status={}", source, result.status());
    }

    private void applyDefendantDetails(Defendant defendant, EnrichedDefendant enrichedDefendant) {
        defendant.setCprUUID(enrichedDefendant.cprUUID());
        if (enrichedDefendant.name() != null) {
            defendant.setName(Name.builder()
                    .title(enrichedDefendant.name().title())
                    .forename1(enrichedDefendant.name().forename1())
                    .forename2(enrichedDefendant.name().forename2())
                    .surname(enrichedDefendant.name().surname())
                    .build());
        }
        Optional.ofNullable(enrichedDefendant.dateOfBirth()).ifPresent(defendant::setDateOfBirth);
        Optional.ofNullable(enrichedDefendant.sex()).ifPresent(defendant::setSex);
        Optional.ofNullable(enrichedDefendant.address()).ifPresent(address -> defendant.setAddress(Address.builder()
                .line1(address.line1())
                .line2(address.line2())
                .line3(address.line3())
                .line4(address.line4())
                .line5(address.line5())
                .postcode(address.postcode())
                .build()));
    }

    private GroupedOffenderMatches mapGroupedMatches(EnrichmentWorkerResponse result) {
        if (result.groupedOffenderMatches() == null) {
            return null;
        }
        List<OffenderMatch> matches = result.groupedOffenderMatches().matches().stream()
                .map(this::mapMatch)
                .collect(Collectors.toList());
        return GroupedOffenderMatches.builder().matches(matches).build();
    }

    private OffenderMatch mapMatch(EnrichedOffenderMatch match) {
        var identifiers = match.matchIdentifiers();
        var aliases = identifiers.aliases() == null ? null : identifiers.aliases().stream()
                .map(alias -> OffenderAlias.builder()
                        .firstName(alias.firstName())
                        .surname(alias.surname())
                        .middleNames(alias.middleNames())
                        .build())
                .collect(Collectors.toList());
        return OffenderMatch.builder()
                .matchIdentifiers(MatchIdentifiers.builder()
                        .crn(identifiers.crn())
                        .pnc(identifiers.pnc())
                        .cro(identifiers.cro())
                        .aliases(aliases)
                        .build())
                .matchType(MatchType.valueOf(match.matchType().name()))
                .confirmed(Boolean.TRUE.equals(match.confirmed()))
                .rejected(Boolean.TRUE.equals(match.rejected()))
                .build();
    }

    private String targetId(Defendant defendant, DataSource source) {
        if (source == DataSource.LIBRA) {
            return defendant.getCId();
        }
        return defendant.getDefendantId();
    }
}
