package uk.gov.justice.probation.courtcasematcher.restclient.model.enrichmentworker;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EnrichmentWorkerResponse(
        EnrichmentSourceType sourceType,
        String targetId,
        EnrichmentStatus status,
        EnrichedDefendant defendant,
        EnrichedOffender offender,
        EnrichedProbation probation,
        EnrichedGroupedOffenderMatches groupedOffenderMatches,
        List<String> warnings,
        List<String> errors
) {
    public enum EnrichmentSourceType {
        COMMON_PLATFORM,
        LIBRA
    }

    public enum EnrichmentStatus {
        ENRICHED,
        NO_MATCH,
        PARTIAL,
        FAILED,
        NEEDS_REVIEW
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EnrichedDefendant(
            String cprUUID,
            EnrichedName name,
            LocalDate dateOfBirth,
            String sex,
            EnrichedAddress address
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EnrichedName(String title, String forename1, String forename2, String surname) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EnrichedAddress(String line1, String line2, String line3, String line4, String line5, String postcode) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EnrichedOffender(String crn, String pnc, String cro) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EnrichedProbation(
            String probationStatus,
            Boolean awaitingPsr,
            Boolean breach,
            LocalDate previouslyKnownTerminationDate,
            Boolean preSentenceActivity
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EnrichedGroupedOffenderMatches(List<EnrichedOffenderMatch> matches) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EnrichedOffenderMatch(
            EnrichedMatchIdentifiers matchIdentifiers,
            EnrichedMatchType matchType,
            Boolean confirmed,
            Boolean rejected
    ) {}

    public enum EnrichedMatchType {
        NAME_DOB_ALIAS
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EnrichedMatchIdentifiers(String crn, String pnc, String cro, List<EnrichedAlias> aliases) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EnrichedAlias(String firstName, String surname, List<String> middleNames) {}
}
