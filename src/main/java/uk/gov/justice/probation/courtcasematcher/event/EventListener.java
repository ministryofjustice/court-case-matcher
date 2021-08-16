package uk.gov.justice.probation.courtcasematcher.event;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@Slf4j
public class EventListener {

    @Autowired
    public EventListener(EventBus eventBus) {
        super();
        eventBus.register(this);
    }

    @AllowConcurrentEvents
    @Subscribe
    public void courtCaseEvent(CourtCaseFailureEvent courtCaseEvent) {
        log.error("Message processing failed. Error: {} ", courtCaseEvent.getFailureMessage(), courtCaseEvent.getThrowable());
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
        log.info("EventBus success event for posting case {} for court {}. ", caseNo, court);
    }
}
