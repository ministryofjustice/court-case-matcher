package uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.messaging.CprExtractor;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.justice.probation.courtcasematcher.model.domain.DataSource.COMMON_PLATFORM;

@ExtendWith(MockitoExtension.class)
class CPHearingTest {

    @Mock
    private CprExtractor cprExtractor;

    @Test
    void mapToDomain() {
        final var commonPlatformHearing = buildHearing();
        final var hearings = commonPlatformHearing.asDomain(cprExtractor);

        applyAssertions(hearings.getFirst());
    }

    @Test
    void mapToDomainIncludingCprFields() {
        final var commonPlatformHearing = buildHearing();
        when(cprExtractor.canExtractCprFields(anyString())).thenReturn(true);

        final var hearings = commonPlatformHearing.asDomain(cprExtractor);
        applyAssertions(hearings.getFirst());
    }

    static void applyAssertions(Hearing hearing) {
        assertThat(hearing.getCaseId()).isEqualTo("E4631566-6479-4EBA-BFFA-DD599147FBAB");
        assertThat(hearing.getCaseNo()).isNull();
        assertThat(hearing.getCourtCode()).isEqualTo("B10JQ");
        assertThat(hearing.getSource()).isEqualTo(COMMON_PLATFORM);
        assertThat(hearing.getUrn()).isEqualTo("urn");
        assertThat(hearing.getHearingType()).isEqualTo("sentence");

        final var firstDefendant = hearing.getDefendants().get(0);
        assertThat(firstDefendant.getDefendantId()).isEqualTo("92673716-9B4F-40B2-A3E5-B398750328E9");
        assertThat(firstDefendant.getCro()).isEqualTo("cro");
        assertThat(firstDefendant.getPnc()).isEqualTo("pncid");
        assertThat(firstDefendant.getSex()).isEqualTo("MALE");
        assertThat(firstDefendant.getName()).isEqualTo(Name.builder()
                .title("title")
                .forename1("firstname")
                .forename2("middlename")
                .surname("lastname")
                .build());
        assertThat(firstDefendant.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(firstDefendant.getOffences().get(0).getId()).isEqualTo("7AA4F55F-F104-4D07-9732-FEB679230E49");
        assertThat(firstDefendant.getOffences().get(0).getOffenceCode()).isEqualTo("ABC001");
        assertThat(firstDefendant.getOffences().get(1).getId()).isEqualTo("43AE14D2-6980-49DD-803B-B6A77E7D438E");
        assertThat(firstDefendant.getOffences().get(1).getOffenceCode()).isEqualTo("EDF002");

        final var secondDefendant = hearing.getDefendants().get(1);
        assertThat(secondDefendant.getDefendantId()).isEqualTo("80F2F0CB-11F3-4A65-B587-A9808BC74C02");
        assertThat(secondDefendant.getCro()).isNull();
        assertThat(secondDefendant.getPnc()).isNull();
        assertThat(secondDefendant.getName()).isEqualTo(Name.builder()
                .surname("organisationname")
                .build());
        assertThat(secondDefendant.getOffences().get(0).getId()).isEqualTo("2B6AAC03-FEFD-41E9-87C2-7B3E8B8F27D9");
        assertThat(secondDefendant.getOffences().get(0).getOffenceCode()).isEqualTo("ABC001");

        assertThat(hearing.getHearingDays().get(0).getListNo()).isNull();
        assertThat(hearing.getHearingDays().get(1).getListNo()).isNull();
    }

    static CPHearing buildHearing() {
        return CPHearing.builder()
                .id("F40CF1F4-B226-44C8-A377-494AEC9F4EBE")
                .type(CPHearingType.builder().description("sentence").build())
                .jurisdictionType(CPJurisdictionType.MAGISTRATES) // Note: Not currently mapped to anything
                .hearingDays(List.of(
                        CPHearingDay.builder()
                                .listedDurationMinutes(10)
                                .sittingDay(LocalDateTime.of(2021, 9, 15, 9, 0))
                                .listingSequence(1)
                                .build(),
                        CPHearingDay.builder()
                                .listedDurationMinutes(10)
                                .sittingDay(LocalDateTime.of(2021, 9, 15, 9, 0))
                                .listingSequence(1)
                                .build()
                ))
                .courtCentre(CPCourtCentre.builder()
                        .id("38AEAC4F-A433-43D8-8896-6812F101405F")
                        .code("B10JQ00")
                        .roomName("Room 1")
                        .build())
                .prosecutionCases(List.of(CPProsecutionCase.builder()
                        .id("E4631566-6479-4EBA-BFFA-DD599147FBAB")
                        .defendants(List.of(
                                buildPersonDefendant(),
                                buildOrganisationDefendant()
                        ))
                        .prosecutionCaseIdentifier(ProsecutionCaseIdentifier.builder().caseUrn("urn").build())
                        .build())
                )
                .build();
    }

    static CPDefendant buildPersonDefendant() {
        return CPDefendant.builder()
                .id("92673716-9B4F-40B2-A3E5-B398750328E9")
                .croNumber("cro")
                .pncId("pncid")
                .prosecutionCaseId("E4631566-6479-4EBA-BFFA-DD599147FBAB")
                .personDefendant(CPPersonDefendant.builder()
                        .personDetails(CPPersonDetails.builder()
                                .gender("MALE")
                                .title("title")
                                .firstName("firstname")
                                .middleName("middlename")
                                .lastName("lastname")
                                .dateOfBirth(LocalDate.of(2000, 1, 1))
                                .build())
                        .build())
                .offences(List.of(CPOffence.builder()
                                .id("7AA4F55F-F104-4D07-9732-FEB679230E49")
                                .wording("wording")
                                .offenceTitle("title")
                                .offenceLegislation("legislation")
                                .offenceCode("ABC001")
                                .judicialResults(List.of(CPJudicialResult.builder()
                                        .judicialResultTypeId("judicialResultTypeId")
                                        .build()))
                                .build(),
                        CPOffence.builder()
                                .id("43AE14D2-6980-49DD-803B-B6A77E7D438E")
                                .wording("wording2")
                                .offenceTitle("title2")
                                .offenceLegislation("legislation2")
                                .offenceCode("EDF002")
                                .judicialResults(List.of(CPJudicialResult.builder()
                                        .judicialResultTypeId("judicialResultTypeId")
                                        .build()))
                                .build())
                ).build();
    }

    static CPDefendant buildOrganisationDefendant() {
        return CPDefendant.builder()
                .id("80F2F0CB-11F3-4A65-B587-A9808BC74C02")
                .prosecutionCaseId("E4631566-6479-4EBA-BFFA-DD599147FBAB")
                .legalEntityDefendant(CPLegalEntityDefendant.builder()
                        .organisation(CPOrganisation.builder()
                                .name("organisationname")
                                .build())
                        .build())
                .offences(List.of(CPOffence.builder()
                        .id("2B6AAC03-FEFD-41E9-87C2-7B3E8B8F27D9")
                        .wording("wording3")
                        .offenceTitle("title3")
                        .offenceLegislation("legislation3")
                        .offenceCode("ABC001")
                        .judicialResults(List.of(CPJudicialResult.builder()
                                .judicialResultTypeId("judicialResultTypeId")
                                .build()))
                        .build())
                ).build();

    }
}
