package uk.gov.justice.probation.courtcasematcher.messaging;

import javax.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.springframework.util.CollectionUtils;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

public interface MessageProcessor {

    default CourtCaseFailureEvent handleException(Exception ex, String payload) {
        CourtCaseFailureEvent.CourtCaseFailureEventBuilder builder = CourtCaseFailureEvent.builder()
            .failureMessage(ex.getMessage())
            .throwable(ex)
            .incomingMessage(payload);
        if (ex instanceof ConstraintViolationException) {
            builder.violations(((ConstraintViolationException)ex).getConstraintViolations());
        }
        return builder.build();
    }

    default void logErrors(Logger log, CourtCaseFailureEvent courtCaseFailureEvent) {
        log.error("Message processing failed. Error: {} ", courtCaseFailureEvent.getFailureMessage(), courtCaseFailureEvent.getThrowable());
        if (!CollectionUtils.isEmpty(courtCaseFailureEvent.getViolations())) {
            courtCaseFailureEvent.getViolations().forEach(
                cv -> log.error("Validation failed : {} at {} ", cv.getMessage(), cv.getPropertyPath().toString())
            );
        }
    }

    default String saveCase(Case aCase, String messageId) {
        getTelemetryService().trackCourtCaseEvent(aCase, messageId);
        getCourtCaseService().getCourtCase(aCase)
            .subscribe(this::postCaseEvent);
        return aCase.getCaseNo();
    }

    void postCaseEvent(CourtCase courtCase);

    void process(String payload, String messageId);

    TelemetryService getTelemetryService();

    CourtCaseService getCourtCaseService();
}
