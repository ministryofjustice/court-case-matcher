package uk.gov.justice.probation.courtcasematcher.model.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum HearingEventType {
    CONFIRMED_OR_UPDATED("ConfirmedOrUpdated"),
    RESULTED("Resulted"),
    UNKNOWN("Unknown");

    private static final HearingEventType DEFAULT = CONFIRMED_OR_UPDATED;

    final String type;

    HearingEventType(String type) {
        this.type = type;
    }

    @JsonCreator
    public static HearingEventType of(String hearingEventType) {
        hearingEventType = hearingEventType == null ? DEFAULT.name() : hearingEventType;
        switch (hearingEventType) {
            case "ConfirmedOrUpdated":
                return CONFIRMED_OR_UPDATED;
            case "Resulted":
                return RESULTED;
            default:
                log.warn("Unknown hearing event type received {}. Returning CONFIRMED_OR_UPDATED.", hearingEventType);
                return DEFAULT;
        }
    }
}
