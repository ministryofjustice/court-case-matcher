package uk.gov.justice.probation.courtcasematcher.model.mapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraAddress;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraCase;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraName;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraOffence;
import uk.gov.justice.probation.courtcasematcher.model.domain.Address;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase.CourtCaseBuilder;
import uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.model.domain.MatchIdentifiers;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;
import uk.gov.justice.probation.courtcasematcher.model.domain.OffenderMatch;
import uk.gov.justice.probation.courtcasematcher.model.domain.ProbationStatusDetail;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchType;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.Offender;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@AllArgsConstructor
@Component
@Slf4j
public class CaseMapper {

    public static CourtCase newFromCase(LibraCase aLibraCase) {
        return newFromLibraCase(aLibraCase)
            .isNew(true)
            .build();
    }

    private static CourtCase.CourtCaseBuilder newFromCourtCase(CourtCase courtCase) {
        return CourtCase.builder()
            .caseNo(courtCase.getCaseNo())
            .courtCode(courtCase.getCourtCode())
            .caseId(String.valueOf(courtCase.getCaseId()))
            .courtRoom(courtCase.getCourtRoom())
            .defendantAddress(courtCase.getDefendantAddress())
            .defendantName(courtCase.getDefendantName())
            .name(courtCase.getName())
            .defendantDob(courtCase.getDefendantDob())
            .defendantSex(courtCase.getDefendantSex())
            .defendantType(courtCase.getDefendantType())
            .cro(courtCase.getCro())
            .pnc(courtCase.getPnc())
            .listNo(courtCase.getListNo())
            .sessionStartTime(courtCase.getSessionStartTime())
            .nationality1(courtCase.getNationality1())
            .nationality2(courtCase.getNationality2())
            .preSentenceActivity(courtCase.isPreSentenceActivity())
            .offences(courtCase.getOffences());
    }

    private static CourtCase.CourtCaseBuilder newFromLibraCase(LibraCase aLibraCase) {
        return CourtCase.builder()
            .caseNo(aLibraCase.getCaseNo())
            .courtCode(aLibraCase.getCourtCode())
            .caseId(String.valueOf(aLibraCase.getCaseId()))
            .courtRoom(aLibraCase.getCourtRoom())
            .defendantAddress(Optional.ofNullable(aLibraCase.getDefendantAddress()).map(CaseMapper::fromAddress).orElse(null))
            .name(Optional.ofNullable(aLibraCase.getName()).map(LibraName::asDomain).orElse(null))
            .defendantName(nameFrom(aLibraCase.getDefendantName(), aLibraCase.getName()))
            .defendantDob(aLibraCase.getDefendantDob())
            .defendantSex(aLibraCase.getDefendantSex())
            .defendantType(DefendantType.of(aLibraCase.getDefendantType()))
            .cro(aLibraCase.getCro())
            .pnc(aLibraCase.getPnc())
            .listNo(aLibraCase.getListNo())
            .sessionStartTime(aLibraCase.getSessionStartTime())
            .nationality1(aLibraCase.getNationality1())
            .nationality2(aLibraCase.getNationality2())
            .offences(Optional.ofNullable(aLibraCase.getOffences()).map(CaseMapper::fromOffences).orElse(Collections.emptyList()));
    }

    private static List<Offence> fromOffences(List<LibraOffence> offences) {
        return Optional.ofNullable(offences)
                        .map(offs -> offs.stream()
                            .sorted(comparing(LibraOffence::getSeq))
                            .map(offence -> Offence.builder()
                                .offenceTitle(offence.getTitle())
                                .offenceSummary(offence.getSummary())
                                .sequenceNumber(offence.getSeq())
                                .act(offence.getAct())
                                .build())
                            .collect(Collectors.toList()))
                        .orElse(Collections.emptyList());
    }

    public static Address fromAddress(LibraAddress def_addr) {
        return Optional.ofNullable(def_addr)
                        .map(address -> Address.builder()
                                        .line1(def_addr.getLine1())
                                        .line2(def_addr.getLine2())
                                        .line3(def_addr.getLine3())
                                        .line4(def_addr.getLine4())
                                        .line5(def_addr.getLine5())
                                        .postcode(def_addr.getPcode())
                                        .build())
            .orElse(null);
    }

    public static String nameFrom(String defendantName, LibraName libraName) {
        return Optional.ofNullable(defendantName)
            .orElse(Optional.ofNullable(libraName)
                .map(LibraName::getFullName)
                .orElse(null));
    }

    public static String nameFrom(String defendantName, Name name) {
        return Optional.ofNullable(defendantName)
            .orElse(Optional.ofNullable(name)
                .map(Name::getFullName)
                .orElse(null));
    }

    public static CourtCase merge(CourtCase incomingCase, CourtCase existingCourtCase) {
        return CourtCase.builder()
            // PK fields
            .courtCode(incomingCase.getCourtCode())
            .caseNo(existingCourtCase.getCaseNo())
            // Fields to be updated from incoming
            .caseId(String.valueOf(incomingCase.getCaseId()))
            .courtRoom(incomingCase.getCourtRoom())
            .defendantAddress(incomingCase.getDefendantAddress())
            .name(incomingCase.getName())
            .defendantName(nameFrom(incomingCase.getDefendantName(), incomingCase.getName()))
            .defendantSex(incomingCase.getDefendantSex())
            .defendantDob(incomingCase.getDefendantDob())
            .defendantType(incomingCase.getDefendantType())
            .listNo(incomingCase.getListNo())
            .sessionStartTime(incomingCase.getSessionStartTime())
            .offences(incomingCase.getOffences())
            .nationality1(incomingCase.getNationality1())
            .nationality2(incomingCase.getNationality2())
            // Fields to be retained from existing court case
            .breach(existingCourtCase.getBreach())
            .previouslyKnownTerminationDate(existingCourtCase.getPreviouslyKnownTerminationDate())
            .crn(existingCourtCase.getCrn())
            .probationStatus(existingCourtCase.getProbationStatusActual())
            .probationStatusActual(existingCourtCase.getProbationStatusActual())
            .suspendedSentenceOrder(existingCourtCase.getSuspendedSentenceOrder())
            .pnc(existingCourtCase.getPnc())

            .build();
    }

    public static CourtCase merge(ProbationStatusDetail probationStatusDetail, CourtCase existingCourtCase) {
        return CourtCase.builder()
            // Fields to be replaced from new probation status detail
            .breach(probationStatusDetail.getInBreach())
            .preSentenceActivity(probationStatusDetail.isPreSentenceActivity())
            .previouslyKnownTerminationDate(probationStatusDetail.getPreviouslyKnownTerminationDate())
            .probationStatus(probationStatusDetail.getStatus())
            .awaitingPsr(probationStatusDetail.isAwaitingPsr())
            // PK fields
            .courtCode(existingCourtCase.getCourtCode())
            .caseNo(existingCourtCase.getCaseNo())
            // Fields to be retained
            .caseId(String.valueOf(existingCourtCase.getCaseId()))
            .crn(existingCourtCase.getCrn())
            .cro(existingCourtCase.getCro())
            .courtRoom(existingCourtCase.getCourtRoom())
            .defendantAddress(existingCourtCase.getDefendantAddress())
            .name(existingCourtCase.getName())
            .defendantName(existingCourtCase.getDefendantName())
            .defendantSex(existingCourtCase.getDefendantSex())
            .defendantDob(existingCourtCase.getDefendantDob())
            .defendantType(existingCourtCase.getDefendantType())
            .listNo(existingCourtCase.getListNo())
            .pnc(existingCourtCase.getPnc())
            .sessionStartTime(existingCourtCase.getSessionStartTime())
            .suspendedSentenceOrder(existingCourtCase.getSuspendedSentenceOrder())
            .offences(existingCourtCase.getOffences())
            .nationality1(existingCourtCase.getNationality1())
            .nationality2(existingCourtCase.getNationality2())
            .build();
    }

    public static CourtCase newFromCourtCaseWithMatches(CourtCase incomingCase, MatchDetails matchDetails) {

        CourtCaseBuilder courtCaseBuilder = newFromCourtCase(incomingCase)
            .groupedOffenderMatches(buildGroupedOffenderMatch(matchDetails.getMatches(), matchDetails.getMatchType()));

        if (matchDetails.isExactMatch()) {
            Offender offender = matchDetails.getMatches().get(0).getOffender();
            ProbationStatusDetail probationStatus = offender.getProbationStatus();
            courtCaseBuilder
                .breach(Optional.ofNullable(probationStatus).map(ProbationStatusDetail::getInBreach).orElse(null))
                .previouslyKnownTerminationDate(
                    Optional.ofNullable(probationStatus).map(ProbationStatusDetail::getPreviouslyKnownTerminationDate).orElse(null))
                .probationStatus(Optional.ofNullable(probationStatus).map(ProbationStatusDetail::getStatus).orElse(null))
                .probationStatusActual(Optional.ofNullable(probationStatus).map(ProbationStatusDetail::getStatus).orElse(null))
                .preSentenceActivity(Optional.ofNullable(probationStatus).map(ProbationStatusDetail::isPreSentenceActivity).orElse(false))
                .awaitingPsr(Optional.ofNullable(probationStatus).map(ProbationStatusDetail::isAwaitingPsr).orElse(false))
                .crn(offender.getOtherIds().getCrn())
                .cro(offender.getOtherIds().getCroNumber())
                .pnc(offender.getOtherIds().getPncNumber())
                .build();
        }

        return courtCaseBuilder.build();
    }

    private static GroupedOffenderMatches buildGroupedOffenderMatch(List<Match> matches, MatchType matchType) {

        if (matches == null || matches.isEmpty()) {
            return GroupedOffenderMatches.builder().matches(Collections.emptyList()).build();
        }
        return GroupedOffenderMatches.builder()
            .matches(matches.stream()
                .map(match -> buildOffenderMatch(matchType, match))
                .collect(Collectors.toList()))
            .build();
    }

    private static OffenderMatch buildOffenderMatch(MatchType matchType, Match match) {
        return OffenderMatch.builder()
            .rejected(false)
            .confirmed(false)
            .matchType(matchType)
            .matchIdentifiers(MatchIdentifiers.builder()
                .pnc(match.getOffender().getOtherIds().getPncNumber())
                .cro(match.getOffender().getOtherIds().getCroNumber())
                .crn(match.getOffender().getOtherIds().getCrn())
                .build())
            .build();
    }
}
