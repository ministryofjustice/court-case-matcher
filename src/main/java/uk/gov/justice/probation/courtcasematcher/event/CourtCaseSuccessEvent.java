package uk.gov.justice.probation.courtcasematcher.event;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseserviceapi.CourtCaseApi;

@Getter
@Slf4j
@Builder
public class CourtCaseSuccessEvent {

    private CourtCaseApi courtCaseApi;

}
