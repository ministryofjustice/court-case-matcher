package uk.gov.justice.probation.courtcasematcher.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;
import uk.gov.justice.probation.courtcasematcher.repository.CourtCaseRepository;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class CourtCaseService {

    @Autowired
    @Qualifier("court-case-rest-client")
    private CourtCaseRepository courtCaseRepository;

    @Autowired
    private OffenderSearchRestClient offenderSearchRestClient;

    public Mono<CourtCase> getCourtCase(CourtCase aCase) {
        if (aCase.getCaseNo() == null) {
            log.warn(String.format("caseNo not available for %s case with id '%s'. Skipping check for existing case.", aCase.getSource(), aCase.getCaseId()));
            return Mono.just(aCase);
        }
        return courtCaseRepository.getCourtCase(aCase.getCourtCode(), aCase.getCaseNo())
                .map(existing -> CaseMapper.merge(aCase, existing))
                .switchIfEmpty(Mono.defer(() -> Mono.just(aCase)));
    }

    public void saveCourtCase(CourtCase courtCase) {
        CourtCase updatedCase = courtCase;
        // New LIBRA cases will have no case or defendant ID and we need to assign
        if (courtCase.getCaseId() == null) {
            updatedCase = assignUuids(courtCase);
        } else if (courtCase.getCaseNo() == null) {
            // Retain the case ID if there is one
            final var caseId = courtCase.getCaseId() != null ? courtCase.getCaseId() : UUID.randomUUID().toString();
            updatedCase = courtCase.withCaseId(caseId)
                    .withCaseNo(caseId);
        }

        try {
            courtCaseRepository.putCourtCase(updatedCase)
                    .block();
        } finally {
            courtCaseRepository.postOffenderMatches(updatedCase.getCaseId(), updatedCase.getDefendants())
                    .block();
        }
    }

    public Mono<CourtCase> updateProbationStatusDetail(CourtCase courtCase) {
        final var updatedDefendants = courtCase.getDefendants()
                .stream()
                .map(defendant -> defendant.getCrn() != null ? updateDefendant(defendant) : Mono.just(defendant))
                .map(Mono::block)
                .collect(Collectors.toList());

        return Mono.just(courtCase.withDefendants(updatedDefendants));
    }

    private Mono<Defendant> updateDefendant(Defendant defendant) {
        return offenderSearchRestClient.search(defendant.getCrn())
                .filter(searchResponses -> searchResponses.getSearchResponses().size() == 1)
                .map(searchResponses -> searchResponses.getSearchResponses().get(0).getProbationStatusDetail())
                .map(probationStatusDetail -> CaseMapper.merge(probationStatusDetail, defendant))
                .defaultIfEmpty(defendant);
    }

    CourtCase assignUuids(CourtCase courtCase) {
        // Apply the new case ID
        final var caseId = UUID.randomUUID().toString();
        var updatedCase = courtCase.withCaseId(caseId);

        // We want to retain the LIBRA case no if present
        if (courtCase.getCaseNo() == null) {
            updatedCase = updatedCase.withCaseNo(caseId);
        }

        // Assign defendant IDs
        final var updatedDefendants = courtCase.getDefendants()
                .stream()
                .map(defendant -> defendant.withDefendantId(
                        defendant.getDefendantId() == null ? UUID.randomUUID().toString() : defendant.getDefendantId()
                ))
                .collect(Collectors.toList());
        updatedCase = updatedCase.withDefendants(updatedDefendants);

        return updatedCase;
    }

}
