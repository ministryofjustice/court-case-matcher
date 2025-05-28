package uk.gov.justice.probation.courtcasematcher.service;

import com.nimbusds.oauth2.sdk.util.ListUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.Address;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.model.domain.MatchIdentifiers;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offender;
import uk.gov.justice.probation.courtcasematcher.model.domain.OffenderMatch;
import uk.gov.justice.probation.courtcasematcher.model.domain.ProbationStatusDetail;
import uk.gov.justice.probation.courtcasematcher.model.type.MatchType;
import uk.gov.justice.probation.courtcasematcher.restclient.CprServiceClient;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice.CprAddress;
import uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice.CprAlias;
import uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice.CprDefendant;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OffenderAlias;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OtherIds;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.SearchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.SearchResponses;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor(onConstructor_ = @Autowired)
public class CprService {

    private final CprServiceClient cprServiceClient;
    private final OffenderSearchRestClient offenderSearchRestClient;

    public void updateDefendants(List<Defendant> defendants) {
        defendants.stream()
            .filter(defendant -> defendant.getCprUUID() != null)
            .forEach(this::updateDefendant);
    }

    public void updateDefendant(Defendant defendant) {
        Mono<CprDefendant> cprCanonicalRecord = cprServiceClient.getCprCanonicalRecord(defendant.getCprUUID());
        cprCanonicalRecord.blockOptional()
            .ifPresentOrElse(cprDefendant -> mapCprDefendantToDefendant(defendant, cprDefendant), () -> {});
    }

    private void mapCprDefendantToDefendant(Defendant defendant, CprDefendant cprDefendant) {
        defendant.setName(Name.builder()
            .title(cprDefendant.getTitle().getDescription())
                .forename1(cprDefendant.getFirstName())
                .forename2(cprDefendant.getMiddleNames())
                .surname(cprDefendant.getLastName())
            .build());
        defendant.setDateOfBirth(LocalDate.parse(cprDefendant.getDateOfBirth(), DateTimeFormatter.ISO_DATE));
        defendant.setSex(cprDefendant.getSex().getDescription());
        setLatestAddress(defendant, cprDefendant);
        setDefendantDetailsWhenExactMatch(defendant, cprDefendant);
        defendant.setGroupedOffenderMatches(buildGroupedOffenderMatch(cprDefendant));
    }

    public void setDefendantDetailsWhenExactMatch(Defendant defendant, CprDefendant cprDefendant) {
        if(cprDefendant.getIdentifiers().getCrns().size() == 1) {
            offenderSearch(cprDefendant.getIdentifiers().getCrns().getFirst()).getSearchResponses().forEach(searchResponse ->
                    setDefendantProperties(searchResponse.getOtherIds(), searchResponse.getProbationStatusDetail(), defendant)
                );
        }
    }

    private void setDefendantProperties(OtherIds otherIds, ProbationStatusDetail probationStatus, Defendant defendant) {
        defendant.setBreach(Optional.ofNullable(probationStatus).map(ProbationStatusDetail::getInBreach).orElse(null));
        defendant.setPreviouslyKnownTerminationDate(Optional.ofNullable(probationStatus).map(ProbationStatusDetail::getPreviouslyKnownTerminationDate).orElse(null));
        defendant.setProbationStatus(Optional.ofNullable(probationStatus).map(ProbationStatusDetail::getStatus).orElse(null));
        defendant.setPreSentenceActivity(probationStatus != null && probationStatus.isPreSentenceActivity());
        defendant.setAwaitingPsr(Optional.ofNullable(probationStatus).map(ProbationStatusDetail::isAwaitingPsr).orElse(false));
        defendant.setCrn(otherIds.getCrn());
        defendant.setOffender(
                otherIds.getPncNumber() != null || otherIds.getCroNumber() != null ? Offender.builder()
                    .pnc(otherIds.getPncNumber())
                    .cro(otherIds.getCroNumber())
                    .build() : null
            );
    }

    private void setLatestAddress(Defendant defendant, CprDefendant cprDefendant) {
        Optional<CprAddress> latestAddress = cprDefendant.getAddresses().stream()
            .filter(cprAddress -> cprAddress.getEndDate() == null || cprAddress.getEndDate().isEmpty())
            .findFirst();
        latestAddress.ifPresent(cprAddress -> defendant.setAddress(Address.builder()
                .line1(cprAddress.getBuildingName())
                .line2(cprAddress.getBuildingNumber())
                .line3(cprAddress.getThoroughfareName())
                .line4(cprAddress.getDependentLocality())
                .line5(cprAddress.getPostTown())
                .postcode(cprAddress.getPostcode())
            .build()));
    }

    public GroupedOffenderMatches buildGroupedOffenderMatch(CprDefendant cprDefendant) {
        if (!cprDefendant.getIdentifiers().getCrns().isEmpty()) {
            return GroupedOffenderMatches.builder()
                .matches(cprDefendant.getIdentifiers().getCrns().stream()
                    .map(crn -> buildOffenderMatch(crn, cprDefendant.getAliases())).toList())
                .build();
        }
        return null;
    }

    private OffenderMatch buildOffenderMatch(String crn, List<CprAlias> aliases) {
        List<SearchResponse> searchResponses = offenderSearch(crn).getSearchResponses();
        OtherIds otherIds = !searchResponses.isEmpty() ? searchResponses.getFirst().getOtherIds() : null;
        return OffenderMatch.builder()
            .matchIdentifiers(MatchIdentifiers.builder()
                .crn(crn)
                .pnc(otherIds != null ? otherIds.getPncNumber() : null)
                .cro(otherIds != null ? otherIds.getCroNumber() : null)
                .aliases(setAliases(aliases))
                .build())
            .matchType(MatchType.NAME_DOB_ALIAS)
            .confirmed(false)
            .rejected(false)
            .build();
    }

    private List<OffenderAlias> setAliases(List<CprAlias> aliases) {
        return !CollectionUtils.isEmpty(aliases) ?  aliases.stream()
            .map(cprAlias -> OffenderAlias.builder()
                .firstName(cprAlias.getFirstName())
                .surname(cprAlias.getLastName())
                .middleNames(List.of(cprAlias.getMiddleNames()))
                .build()).collect(Collectors.toList()) : null;
    }


    private SearchResponses offenderSearch(String crn) {
        return offenderSearchRestClient.search(crn).block();
    }
}
