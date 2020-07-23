package uk.gov.justice.probation.courtcasematcher.service;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Block;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourtCaseServiceTest {

    @Mock
    private CourtCaseRestClient restClient;

    @InjectMocks
    private CourtCaseService courtCaseService;

    @Test
    void purgeAbsent() {

        LocalDate jan1 = LocalDate.of(2020, Month.JANUARY, 1);
        LocalDate jan2 = LocalDate.of(2020, Month.JANUARY, 2);
        LocalDate jan3 = LocalDate.of(2020, Month.JANUARY, 3);

        Session session1Jan = Session.builder().dateOfHearing(jan1).courtCode("SHF").build();
        Session session2Jan = Session.builder().dateOfHearing(jan2).courtCode("SHF").build();
        Session session3Jan = Session.builder().dateOfHearing(jan3).courtCode("SHF").build();

        Case aCase10 = Case.builder().caseNo("1010").block(Block.builder().session(session1Jan).build()).build();
        Case aCase20 = Case.builder().caseNo("1020").block(Block.builder().session(session2Jan).build()).build();
        Case aCase21 = Case.builder().caseNo("1021").block(Block.builder().session(session2Jan).build()).build();
        Case aCase30 = Case.builder().caseNo("1030").block(Block.builder().session(session3Jan).build()).build();
        Case aCase31 = Case.builder().caseNo("1031").block(Block.builder().session(session3Jan).build()).build();

        List<Case> allCases = asList(aCase20, aCase21, aCase10, aCase30, aCase31);

        courtCaseService.purgeAbsent("SHF", allCases);

        Map<LocalDate, List<String>> expected = Map.of(jan1, asList("1010"), jan2, asList("1020", "1021"), jan3, asList("1030", "1031"));
        verify(restClient).purgeAbsent("SHF", expected);
    }

    @Disabled
    @Test
    void purgeAbsentNoCases() {

        LocalDate jan1 = LocalDate.of(2020, Month.JANUARY, 1);

        courtCaseService.purgeAbsent("SHF", Collections.emptyList());

        Map<LocalDate, List<String>> expected = Map.of(jan1, Collections.emptyList());
        verify(restClient).purgeAbsent("SHF", expected);
    }
}
