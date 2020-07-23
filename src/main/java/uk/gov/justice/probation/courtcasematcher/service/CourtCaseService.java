package uk.gov.justice.probation.courtcasematcher.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Service
@Slf4j
@AllArgsConstructor
public class CourtCaseService {

    @Autowired
    private final CourtCaseRestClient restClient;

    public void purgeAbsent(String courtCode, List<Case> cases) {

        Map<LocalDate, List<String>> casesByDate = cases.stream()
            .collect(groupingBy(aCase -> aCase.getBlock().getSession().getDateOfHearing(), mapping(Case::getCaseNo, toList())));

        restClient.purgeAbsent(courtCode, casesByDate);
    }

}
