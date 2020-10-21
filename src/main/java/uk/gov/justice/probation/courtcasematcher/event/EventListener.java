package uk.gov.justice.probation.courtcasematcher.event;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.OffenderSearchMatchType;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.NameHelper;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

/**
 * We intend to replace EventBus with an external message queue.
 */
@Component
@Slf4j
public class EventListener {

    private final CourtCaseService courtCaseService;

    private final MatcherService matcherService;

    private final TelemetryService telemetryService;

    private final AtomicLong successCount = new AtomicLong(0);

    private final AtomicLong failureCount = new AtomicLong(0);

    @Autowired
    public EventListener(EventBus eventBus,
                        MatcherService matcherService,
                        CourtCaseService courtCaseService,
                        TelemetryService telemetryService) {
        super();
        this.matcherService = matcherService;
        this.courtCaseService = courtCaseService;
        this.telemetryService = telemetryService;
        eventBus.register(this);
    }

    @AllowConcurrentEvents
    @Subscribe
    public void courtCaseEvent(CourtCaseFailureEvent courtCaseEvent) {
        log.error("Message processing failed. Current error count: {}. Error: {} ",
            failureCount.incrementAndGet(), courtCaseEvent.getFailureMessage());
        if (!CollectionUtils.isEmpty(courtCaseEvent.getViolations())) {
            courtCaseEvent.getViolations().forEach(
                cv -> log.error("Validation failed : {} at {} ", cv.getMessage(), cv.getPropertyPath().toString())
            );
        }
    }

    @AllowConcurrentEvents
    @Subscribe
    public void courtCaseEvent(CourtCaseSuccessEvent courtCaseEvent) {
        String caseNo = courtCaseEvent.getCourtCase().getCaseNo();
        String court = courtCaseEvent.getCourtCase().getCourtCode();
        log.info("EventBus success event for posting case {} for court {}. Total count of successful messages {} ",
            caseNo, court, successCount.incrementAndGet());
    }

    @AllowConcurrentEvents
    @Subscribe
    public void courtCaseUpdateEvent(CourtCaseUpdateEvent courtCaseEvent) {
        CourtCase courtCase = courtCaseEvent.getCourtCase();
        log.info("Upsert case no {} for court {}", courtCase.getCaseNo(), courtCase.getCourtCode());
        courtCaseService.saveCourtCase(courtCaseEvent.getCourtCase());
    }

    @AllowConcurrentEvents
    @Subscribe
    public void courtCaseMatchEvent(CourtCaseMatchEvent courtCaseEvent) {

        CourtCase courtCase = courtCaseEvent.getCourtCase();
        log.info("Matching offender and saving case no {} for court {}, defendant name {}",
            courtCase.getCaseNo(), courtCase.getCourtCode(), courtCase.getDefendantName());

        Name name = Optional.ofNullable(courtCase.getName())
                            .orElseGet(() -> NameHelper.getNameFromFields(courtCase.getDefendantName()));

        matcherService.getSearchResponse(name, courtCase.getDefendantDob(), courtCase.getCourtCode(), courtCase.getCaseNo())
            .doOnError(throwable -> {
                telemetryService.trackOffenderMatchFailureEvent(courtCase);
                courtCaseService.createCase(courtCase, SearchResponse.builder()
                                                                    .matchedBy(OffenderSearchMatchType.NOTHING)
                                                                    .matches(Collections.emptyList())
                                                                    .build());
            })
            .subscribe(searchResponse -> {
                telemetryService.trackOffenderMatchEvent(courtCase, searchResponse);
                courtCaseService.createCase(courtCase, searchResponse);
            })
        ;
    }

    public long getSuccessCount() {
        return successCount.get();
    }

    public long getFailureCount() {
        return failureCount.get();
    }
}
