package uk.gov.justice.probation.courtcasematcher.messaging;

import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;

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
            .thenComparing(Defendant::getCro, nullsFirst(naturalOrder()))); //TODO add more fields
    static final Comparator<HearingDay> hearingDayComparator = Comparator.nullsFirst(comparing(HearingDay::getCourtCode, nullsFirst(naturalOrder()))
            .thenComparing(HearingDay::getCourtRoom, nullsFirst(naturalOrder()))
            .thenComparing(HearingDay::getListNo, nullsFirst(naturalOrder()))
            .thenComparing(HearingDay::getSessionStartTime, nullsFirst(naturalOrder())));


    public static boolean hasCourtCaseChanged(CourtCase courtCase, CourtCase courtCaseToCompare) {

        if (hasHearingDayChanged(courtCase.getHearingDays(), courtCaseToCompare.getHearingDays()) ||
                hasDefendantChanged(courtCase.getDefendants(), courtCaseToCompare.getDefendants())) {
            return true;
        }

        return hasCaseChanged(courtCase, courtCaseToCompare);
    }

    private static boolean hasCaseChanged(CourtCase courtCase, CourtCase courtCaseToCompare) {
        return caseComparator.compare(courtCase, courtCaseToCompare) != 0;
    }

    private static boolean hasDefendantChanged(List<Defendant> defendants, List<Defendant> defendantsToCompare) {
        return areNotEqualIgnoringOrder(defendants, defendantsToCompare, defendantComparator);
    }

    private static boolean hasHearingDayChanged(List<HearingDay> hearingDays, List<HearingDay> hearingDaysToCompare) {
        return areNotEqualIgnoringOrder(hearingDays, hearingDaysToCompare, hearingDayComparator);
    }

    private static <T> boolean areNotEqualIgnoringOrder(List<T> list1, List<T> list2, Comparator<? super T> comparator) {

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
