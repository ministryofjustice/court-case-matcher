package uk.gov.justice.probation.courtcasematcher.service;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.Address;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.model.domain.MatchIdentifiers;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;
import uk.gov.justice.probation.courtcasematcher.model.domain.OffenderMatch;
import uk.gov.justice.probation.courtcasematcher.model.type.MatchType;
import uk.gov.justice.probation.courtcasematcher.restclient.CprServiceClient;
import uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice.CprAddress;
import uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice.CprDefendant;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CprService {

    private final CprServiceClient cprServiceClient;

    public CprService(CprServiceClient cprServiceClient) {
        this.cprServiceClient = cprServiceClient;
    }

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
            .title(cprDefendant.getTitle())
                .forename1(cprDefendant.getFirstName())
                .forename2(cprDefendant.getMiddleNames())
                .surname(cprDefendant.getLastName())
            .build());
        defendant.setDateOfBirth(LocalDate.parse(cprDefendant.getDateOfBirth(), DateTimeFormatter.ISO_DATE));
        defendant.setSex(cprDefendant.getSex());
        setLatestAddress(defendant, cprDefendant);
        defendant.setCrn(getCrnForExactMatch(cprDefendant));
        defendant.setProbationStatus(getProbationStatusForExactMatch(cprDefendant));
        defendant.setGroupedOffenderMatches(buildGroupedOffenderMatch(cprDefendant.getIdentifiers().getCrns()));
    }

    //TODO this need to be obtained from somewhere
    private String getProbationStatusForExactMatch(CprDefendant cprDefendant) {
        return cprDefendant.getIdentifiers().getCrns().size() == 1 ?  "CURRENT" : "NO_RECORD";
    }

    private static String getCrnForExactMatch(CprDefendant cprDefendant) {
        return cprDefendant.getIdentifiers().getCrns().size() == 1 ? cprDefendant.getIdentifiers().getCrns().getFirst() : null;
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

    public GroupedOffenderMatches buildGroupedOffenderMatch(List<String> crns) {
        if (!crns.isEmpty()) {
            return GroupedOffenderMatches.builder()
                .matches(crns.stream()
                    .map(CprService::buildOffenderMatch).toList())
                .build();
        }
        return null;
    }

    private static OffenderMatch buildOffenderMatch(String crn) {
        return OffenderMatch.builder()
            .matchIdentifiers(MatchIdentifiers.builder()
                .crn(crn) //TODO need to populate pnc and cro
                .build())
            //TODO these values need to be obtained from somewhere
            .matchType(MatchType.NAME)
            .confirmed(true)
            .rejected(false)
            .build();
    }
}
