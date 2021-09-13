package uk.gov.justice.probation.courtcasematcher.messaging.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum MessageType {
    LIBRA_COURT_CASE,
    COMMON_PLATFORM_HEARING,
    NONE,
    UNKNOWN;

    @JsonCreator
    public static MessageType of(@JsonProperty("Value") String messageType) {
        if (messageType ==  null){
            log.warn("Expected message type but was null");
            return NONE;
        }
        return switch (messageType) {
            case "LIBRA_COURT_CASE" -> LIBRA_COURT_CASE;
            case "CP_TEST_COURT_CASE", "COMMON_PLATFORM_HEARING" -> COMMON_PLATFORM_HEARING;
            default -> {
                log.warn(String.format("Attempt to parse unknown message type '%s'", messageType));
                yield UNKNOWN;
            }
        };
    }
}
