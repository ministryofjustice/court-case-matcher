package uk.gov.justice.probation.courtcasematcher.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ErrorHandler;

@Slf4j
@Service
public class JmsErrorHandler implements ErrorHandler {

    @Override
    public void handleError(Throwable throwable) {
        log.error("Unexpected error processing message", throwable);
    }

}
