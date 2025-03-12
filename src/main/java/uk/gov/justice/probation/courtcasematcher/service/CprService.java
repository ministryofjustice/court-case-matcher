package uk.gov.justice.probation.courtcasematcher.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.Address;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;
import uk.gov.justice.probation.courtcasematcher.restclient.CprServiceClient;
import uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice.CprAddress;
import uk.gov.justice.probation.courtcasematcher.restclient.model.cprservice.CprDefendant;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Slf4j
public class CprService {

    private final CprServiceClient cprServiceClient;

    public CprService(CprServiceClient cprServiceClient) {
        this.cprServiceClient = cprServiceClient;
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
        defendant.setDateOfBirth(LocalDate.parse(cprDefendant.getDateOfBirth(), DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        defendant.setSex(cprDefendant.getSex());
        setLatestAddress(defendant, cprDefendant);
    }

    private static void setLatestAddress(Defendant defendant, CprDefendant cprDefendant) {
        Optional<CprAddress> latestAddress = cprDefendant.getAddresses().stream()
            .filter(cprAddress -> cprAddress.getEndDate() == null || cprAddress.getEndDate().isEmpty())
            .findFirst();
        latestAddress.ifPresent(cprAddress -> defendant.setAddress(Address.builder()
                .line1(cprAddress.getBuildingName() != null ?
                    cprAddress.getBuildingNumber() + ", " + cprAddress.getBuildingName() :
                     cprAddress.getBuildingNumber())
                .line2(cprAddress.getThoroughfareName())
                .line3(cprAddress.getDependentLocality())
                .line4(cprAddress.getPostTown())
                .line5(cprAddress.getCounty())
                .postcode(cprAddress.getPostcode())
            .build()));
    }
}
