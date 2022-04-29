package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;
import uk.gov.justice.probation.courtcasematcher.pact.DomainDataHelper;

import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CCSCourtCaseTest {
    @Test
    public void mapBack() {
        final var original = DomainDataHelper.aCourtCaseWithAllFields();

        final var firstDefendant = original.getDefendants().get(0);
        final var ccsCourtCase = CCSCourtCase.builder()
                .source(CCSDataSource.of(original.getSource()))
                .defendantId(firstDefendant.getDefendantId())
                .awaitingPsr(firstDefendant.getAwaitingPsr())
                .breach(firstDefendant.getBreach())
                .caseId(original.getCaseId())
                .hearingId(original.getHearingId())
                .caseNo(original.getCaseNo())


                .courtCode(original.getCourtCode())
                .courtRoom(original.getFirstHearingDay().map(HearingDay::getCourtRoom).orElse(null))
                .sessionStartTime(original.getFirstHearingDay().map(HearingDay::getSessionStartTime).orElse(null))
                .listNo(original.getFirstHearingDay().map(HearingDay::getListNo).orElse(null))

                .crn(firstDefendant.getCrn())
                .cro(firstDefendant.getCro())
                .pnc(firstDefendant.getPnc())
                .preSentenceActivity(firstDefendant.getPreSentenceActivity())
                .previouslyKnownTerminationDate(firstDefendant.getPreviouslyKnownTerminationDate())
                .probationStatusActual(firstDefendant.getProbationStatus())
                .suspendedSentenceOrder(firstDefendant.getSuspendedSentenceOrder())
                .defendantDob(firstDefendant.getDateOfBirth())
                .defendantName(CaseMapper.nameFrom(firstDefendant.getName()))
                .defendantType(CCSDefendantType.of(firstDefendant.getType()))
                .defendantSex(firstDefendant.getSex())

                .name(Optional.ofNullable(firstDefendant.getName())
                        .map(CCSName::of)
                        .orElse(null))
                .defendantAddress(Optional.ofNullable(firstDefendant.getAddress())
                        .map(CCSAddress::of)
                        .orElse(null))
                .offences(Optional.ofNullable(firstDefendant.getOffences())
                        .map(offences -> offences.stream()
                                .map(CCSOffence::of)
                                .collect(Collectors.toList()))
                        .orElse(null))
                .build();

        final var actual = ccsCourtCase.asDomain();

        assertThat(actual).isEqualTo(original);
    }
}
