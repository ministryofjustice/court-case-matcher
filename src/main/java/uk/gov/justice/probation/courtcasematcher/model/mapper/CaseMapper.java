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
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;
import uk.gov.justice.probation.courtcasematcher.model.domain.MatchIdentifiers;
import uk.gov.justice.probation.courtcasematcher.model.domain.MatchType;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offender;
import uk.gov.justice.probation.courtcasematcher.model.domain.OffenderMatch;
import uk.gov.justice.probation.courtcasematcher.model.domain.ProbationStatusDetail;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OSOffender;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

@AllArgsConstructor
@Component
@Slf4j
public class CaseMapper {

    public static CourtCase newFromLibraCase(LibraCase aLibraCase) {
        return CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode(aLibraCase.getCourtCode())
                        .courtRoom(aLibraCase.getCourtRoom())
                        .sessionStartTime(aLibraCase.getSessionStartTime())
                        .listNo(aLibraCase.getListNo())
                        .build()))
                .defendants(Collections.singletonList(Defendant.builder()
                        .name(Optional.ofNullable(aLibraCase.getName()).map(LibraName::asDomain).orElse(null))
                        .address(Optional.ofNullable(aLibraCase.getDefendantAddress()).map(CaseMapper::fromAddress).orElse(null))
                        .dateOfBirth(aLibraCase.getDefendantDob())
                        .sex(aLibraCase.getDefendantSex())
                        .type(DefendantType.of(aLibraCase.getDefendantType()))
                        .cro(aLibraCase.getCro())
                        .pnc(aLibraCase.getPnc())
                        .offences(Optional.ofNullable(aLibraCase.getOffences()).map(CaseMapper::fromOffences).orElse(Collections.emptyList()))
                        .build()))
                .source(DataSource.LIBRA)
                .caseNo(aLibraCase.getCaseNo())
                .urn(aLibraCase.getUrn())

                .build();
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

    public static CourtCase merge(CourtCase incomingCase, CourtCase existingCourtCase) {
        return existingCourtCase
                .withHearingDays(incomingCase.getHearingDays())
                .withUrn(incomingCase.getUrn())

                // PK fields
                .withCaseNo(existingCourtCase.getCaseNo())
                .withCaseId(Optional.ofNullable(existingCourtCase.getCaseId()).orElse(incomingCase.getCaseId()))

                // Fields to be updated from incoming
                .withDefendants(mergeDefendants(incomingCase.getDefendants(), existingCourtCase.getDefendants(), incomingCase.getSource()));
    }

    private static List<Defendant> mergeDefendants(List<Defendant> incoming, List<Defendant> existingDefendants, DataSource source) {
        return incoming.stream()
                .map(defendant -> merge(defendant, findExistingDefendant(defendant, existingDefendants, source)))
                .collect(Collectors.toList());
    }

    private static Defendant merge(Defendant incoming, Optional<Defendant> optionalExisting) {
        return optionalExisting
                .map(existing -> incoming
                        // Fields to be retained from existing court case
                        .withDefendantId(existing.getDefendantId())
                        .withBreach(existing.getBreach())
                        .withPreviouslyKnownTerminationDate(existing.getPreviouslyKnownTerminationDate())
                        .withCrn(existing.getCrn())
                        .withProbationStatus(existing.getProbationStatus())
                        .withSuspendedSentenceOrder(existing.getSuspendedSentenceOrder())
                        .withPnc(existing.getPnc())
                        .withPreSentenceActivity(existing.getPreSentenceActivity()))
                .orElse(incoming);
    }

    private static Optional<Defendant> findExistingDefendant(Defendant defendant, List<Defendant> existingDefendants, DataSource source) {
        if (source == DataSource.LIBRA) {
            return Optional.ofNullable(existingDefendants)
                    .map(defendants -> defendants.get(0));
        }

        return existingDefendants.stream()
                .filter(existingDefendant -> Objects.equals(existingDefendant.getDefendantId(), defendant.getDefendantId()))
                .findFirst();
    }

    public static Defendant merge(ProbationStatusDetail probationStatusDetail, Defendant existingDefendant) {
        return existingDefendant
                        .withBreach(probationStatusDetail.getInBreach())
                        .withPreSentenceActivity(probationStatusDetail.isPreSentenceActivity())
                        .withPreviouslyKnownTerminationDate(probationStatusDetail.getPreviouslyKnownTerminationDate())
                        .withProbationStatus(probationStatusDetail.getStatus())
                        .withAwaitingPsr(probationStatusDetail.isAwaitingPsr());
    }

    public static Defendant updateDefendantWithMatches(Defendant defendant, MatchResponse matchResponse) {
        final var matchType = Optional.ofNullable(matchResponse)
                .map(MatchResponse::getMatchedBy)
                .map(foo -> foo.asDomain(Optional.ofNullable(defendant.getPnc()).isPresent()))
                .orElse(MatchType.UNKNOWN);

        var newDefendant = defendant
                .withGroupedOffenderMatches(buildGroupedOffenderMatch(matchResponse.getMatches(), matchType));

        if (matchResponse.isExactMatch()) {
            var offender = matchResponse.getMatches().get(0).getOffender();
            var probationStatus = offender.getProbationStatus();
            newDefendant = buildDefendant(offender, newDefendant, probationStatus);
        }

        return newDefendant;
    }

    private static Defendant buildDefendant(OSOffender offender, Defendant defendant, ProbationStatusDetail probationStatus) {
        return defendant
                .withBreach(Optional.ofNullable(probationStatus).map(ProbationStatusDetail::getInBreach).orElse(null))
                .withPreviouslyKnownTerminationDate(
                        Optional.ofNullable(probationStatus).map(ProbationStatusDetail::getPreviouslyKnownTerminationDate).orElse(null))
                .withProbationStatus(Optional.ofNullable(probationStatus).map(ProbationStatusDetail::getStatus).orElse(null))
                .withPreSentenceActivity(probationStatus != null && probationStatus.isPreSentenceActivity())
                .withAwaitingPsr(Optional.ofNullable(probationStatus).map(ProbationStatusDetail::isAwaitingPsr).orElse(false))
                .withCrn(offender.getOtherIds().getCrn())
                .withOffender(
                        offender.getOtherIds().getPncNumber() != null || offender.getOtherIds().getCroNumber() != null ? Offender.builder()
                        .pnc(offender.getOtherIds().getPncNumber())
                        .cro(offender.getOtherIds().getCroNumber())
                        .build() : null
                )
                ;
    }

    public static GroupedOffenderMatches buildGroupedOffenderMatch(List<Match> matches, MatchType matchType) {

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
                        .aliases(match.getOffender().getOffenderAliases())
                        .build())
                .build();
    }

    public static String nameFrom(Name name) {
        return Stream.of(name.getTitle(), name.getForename1(), name.getForename2(), name.getForename3(), name.getSurname())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "))
                .trim();
    }
}
