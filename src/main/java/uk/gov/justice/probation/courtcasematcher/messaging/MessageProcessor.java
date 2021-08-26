package uk.gov.justice.probation.courtcasematcher.messaging;

import org.slf4j.Logger;
import org.springframework.util.CollectionUtils;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import javax.validation.ConstraintViolationException;

/**
 * MessageProcessor
 *
 * The MessageProcessor is responsible for orchestrating the various service calls required as a result of an incoming
 * message. It's important that any errors that should result in a push to the DLQ should throw an exception, which will
 * be passed back up to the caller. As Reactive Monos are asynchronous these must be blocked in order to expose any
 * errors that occurred. Without this blocking behaviour, exceptions will be raised on a separate thread and the caller
 * will not know to push to the DLQ.
 */
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

    default void saveCase(CourtCase aCase, String messageId) {
        getTelemetryService().trackCourtCaseEvent(aCase, messageId);
        final var courtCase = getCourtCaseService().getCourtCase(aCase)
                .block();
        postCaseEvent(courtCase);
    }

    void postCaseEvent(CourtCase courtCase);

    void process(String payload, String messageId);

    TelemetryService getTelemetryService();

    CourtCaseService getCourtCaseService();
}
