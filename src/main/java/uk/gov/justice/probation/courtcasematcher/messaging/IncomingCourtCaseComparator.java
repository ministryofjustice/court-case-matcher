package uk.gov.justice.probation.courtcasematcher.messaging;

import uk.gov.justice.probation.courtcasematcher.model.domain.Address;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;
import uk.gov.justice.probation.courtcasematcher.model.domain.PhoneNumber;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.Objects.isNull;

public class IncomingCourtCaseComparator {

    static final Comparator<CourtCase> caseComparator = Comparator.nullsFirst(comparing(CourtCase::getUrn, nullsFirst(naturalOrder())));

    static final Comparator<Defendant> defendantComparator = Comparator.nullsFirst(comparing(Defendant::getPnc, nullsFirst(naturalOrder()))
            .thenComparing(Defendant::getCro, nullsFirst(naturalOrder()))
            .thenComparing(Defendant::getSex, nullsFirst(naturalOrder()))
            .thenComparing(Defendant::getType, nullsFirst(naturalOrder()))
            .thenComparing(Defendant::getDateOfBirth, nullsFirst(naturalOrder())));

    static final Comparator<Address> addressComparator = Comparator.nullsFirst(comparing(Address::getLine1, nullsFirst(naturalOrder()))
            .thenComparing(Address::getLine2, nullsFirst(naturalOrder()))
            .thenComparing(Address::getLine3, nullsFirst(naturalOrder()))
            .thenComparing(Address::getLine4, nullsFirst(naturalOrder()))
            .thenComparing(Address::getLine5, nullsFirst(naturalOrder()))
            .thenComparing(Address::getPostcode, nullsFirst(naturalOrder())));

    static final Comparator<Name> defendantNameComparator = Comparator.nullsFirst(comparing(Name::getFullName, nullsFirst(naturalOrder())));

    static final Comparator<PhoneNumber> defendantPhoneNumberComparator = Comparator.nullsFirst(comparing(PhoneNumber::getHome, nullsFirst(naturalOrder()))
            .thenComparing(PhoneNumber::getMobile, nullsFirst(naturalOrder()))
            .thenComparing(PhoneNumber::getWork, nullsFirst(naturalOrder())));

    static final Comparator<Offence> offenceComparator = Comparator.nullsFirst(comparing(Offence::getOffenceTitle, nullsFirst(naturalOrder()))
            .thenComparing(Offence::getOffenceSummary, nullsFirst(naturalOrder()))
            .thenComparing(Offence::getAct, nullsFirst(naturalOrder()))
            .thenComparing(Offence::getSequenceNumber, nullsFirst(naturalOrder()))
            .thenComparing(Offence::getListNo, nullsFirst(naturalOrder())));

    static final Comparator<HearingDay> hearingDayComparator = Comparator.nullsFirst(comparing(HearingDay::getCourtCode, nullsFirst(naturalOrder()))
            .thenComparing(HearingDay::getCourtRoom, nullsFirst(naturalOrder()))
            .thenComparing(HearingDay::getListNo, nullsFirst(naturalOrder()))
            .thenComparing(HearingDay::getSessionStartTime, nullsFirst(naturalOrder())));


    public static boolean hasCourtCaseChanged(CourtCase courtCase, CourtCase courtCaseToCompare) {

        if (hasHearingDaysChanged(courtCase.getHearingDays(), courtCaseToCompare.getHearingDays()) ||
                hasDefendantsChanged(courtCase.getDefendants(), courtCaseToCompare.getDefendants()) ||
                hasDefendantOffencesChanged(courtCase.getDefendants(), courtCaseToCompare.getDefendants())) {
            return true;
        }

        return hasCaseChanged(courtCase, courtCaseToCompare);
    }

    private static boolean hasCaseChanged(CourtCase courtCase, CourtCase courtCaseToCompare) {
        return caseComparator.compare(courtCase, courtCaseToCompare) != 0;
    }

    private static boolean hasDefendantsChanged(List<Defendant> defendants, List<Defendant> defendantsToCompare) {
        return areNotEqualIgnoringOrder(defendants, defendantsToCompare, defendantComparator) ||
                hasDefendantNameChanged(defendants, defendantsToCompare) ||
                hasDefendantAddressChanged(defendants, defendantsToCompare) ||
                hasDefendantPhoneNumberChanged(defendants, defendantsToCompare);
    }

    private static boolean hasHearingDaysChanged(List<HearingDay> hearingDays, List<HearingDay> hearingDaysToCompare) {
        return areNotEqualIgnoringOrder(hearingDays, hearingDaysToCompare, hearingDayComparator);
    }

    private static boolean hasDefendantOffencesChanged(List<Defendant> defendants, List<Defendant> defendantsToCompare) {
        return defendants.stream()
                .anyMatch(defendantReceived -> defendantsToCompare.stream()
                        .anyMatch(existingDefendant -> areNotEqualIgnoringOrder(defendantReceived.getOffences(), existingDefendant.getOffences(), offenceComparator)));
    }

    private static boolean hasDefendantAddressChanged(List<Defendant> defendants, List<Defendant> defendantsToCompare) {
        return defendants.stream()
                .anyMatch(defendantReceived -> defendantsToCompare.stream()
                        .anyMatch(existingDefendant -> addressComparator.compare(defendantReceived.getAddress(), existingDefendant.getAddress()) != 0));
    }

    private static boolean hasDefendantNameChanged(List<Defendant> defendants, List<Defendant> defendantsToCompare) {
        return defendants.stream()
                .anyMatch(defendantReceived -> defendantsToCompare.stream()
                        .anyMatch(existingDefendant -> defendantNameComparator.compare(defendantReceived.getName(), existingDefendant.getName()) != 0));
    }

    private static boolean hasDefendantPhoneNumberChanged(List<Defendant> defendants, List<Defendant> defendantsToCompare) {
        return defendants.stream()
                .anyMatch(defendantReceived -> defendantsToCompare.stream()
                        .anyMatch(existingDefendant -> defendantPhoneNumberComparator.compare(defendantReceived.getPhoneNumber(), existingDefendant.getPhoneNumber()) != 0));
    }

    private static <T> boolean areNotEqualIgnoringOrder(List<T> list1, List<T> list2, Comparator<? super T> comparator) {

        // if both are null, nothing has changed
        if (isNull(list1) && isNull(list2)) {
            return false;
        }

        // if any of them is null, then something has changed
        if (isNull(list1) != isNull(list2)) {
            return true;
        }
        // if not the same size, lists has addition or deletion
        if (list1.size() != list2.size()) {
            return true;
        }
        // iterate through the elements and compare them one by one using
        // the provided comparator.
        return IntStream
                .range(0, list1.size())
                .anyMatch(i -> comparator.compare(list1.get(i), list2.get(i)) != 0);
    }
}
