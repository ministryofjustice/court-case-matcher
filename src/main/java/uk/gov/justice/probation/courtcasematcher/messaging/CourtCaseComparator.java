package uk.gov.justice.probation.courtcasematcher.messaging;

import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

public class CourtCaseComparator {
    static final Comparator<CourtCase> caseComparator = Comparator.nullsFirst(comparing(CourtCase::getUrn, nullsFirst(naturalOrder())));
    static final Comparator<Defendant> defendantComparator = Comparator.nullsFirst(comparing(Defendant::getCrn, nullsFirst(naturalOrder()))
            .thenComparing(Defendant::getCro, nullsFirst(naturalOrder()))
            .thenComparing(Defendant::getDateOfBirth, nullsFirst(naturalOrder()))
            .thenComparing(d -> d.getAddress().getLine1(), nullsFirst(naturalOrder()))
            .thenComparing(d -> d.getAddress().getLine2(), nullsFirst(naturalOrder()))
            .thenComparing(d -> d.getAddress().getLine3(), nullsFirst(naturalOrder()))
            .thenComparing(d -> d.getAddress().getLine4(), nullsFirst(naturalOrder()))
            .thenComparing(d -> d.getAddress().getLine5(), nullsFirst(naturalOrder()))
            .thenComparing(d -> d.getAddress().getPostcode(), nullsFirst(naturalOrder())));

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
                hasDefendantsChanged(courtCase.getDefendants(), courtCaseToCompare.getDefendants())) {
            return true;
        }

        return hasCaseChanged(courtCase, courtCaseToCompare);
    }

    private static boolean hasCaseChanged(CourtCase courtCase, CourtCase courtCaseToCompare) {
        return caseComparator.compare(courtCase, courtCaseToCompare) != 0;
    }

    private static boolean hasDefendantsChanged(List<Defendant> defendants, List<Defendant> defendantsToCompare) {
        return areNotEqualIgnoringOrder(defendants, defendantsToCompare, defendantComparator);
    }

    private static boolean hasHearingDaysChanged(List<HearingDay> hearingDays, List<HearingDay> hearingDaysToCompare) {
        return areNotEqualIgnoringOrder(hearingDays, hearingDaysToCompare, hearingDayComparator);
    }

    private static boolean hasDefendantOffencesChanged(List<Offence> offences, List<Offence> offencesToCompare) {
        return areNotEqualIgnoringOrder(offences, offencesToCompare, offenceComparator);
    }

    private static <T> boolean areNotEqualIgnoringOrder(List<T> list1, List<T> list2, Comparator<? super T> comparator) {

        // if either of them is null
        if ((list1 != null && list2 == null) || (list1 == null && list2 != null)) {
            return true;
        }
        // if not the same size, lists has addition or deletion
        if (list1.size() != list2.size()) {
            return true;
        }

        //to avoid modifying the original lists
        List<T> copy1 = new ArrayList<>(list1);
        List<T> copy2 = new ArrayList<>(list2);

        copy1.sort(comparator);
        copy2.sort(comparator);

        // iterate through the elements and compare them one by one using
        // the provided comparator.
        Iterator<T> it1 = copy1.iterator();
        Iterator<T> it2 = copy2.iterator();
        while (it1.hasNext()) {
            T t1 = it1.next();
            T t2 = it2.next();
            if (comparator.compare(t1, t2) != 0) {
                // as soon as a difference is found, stop looping
                return true;
            }
        }
        return false;
    }
}
