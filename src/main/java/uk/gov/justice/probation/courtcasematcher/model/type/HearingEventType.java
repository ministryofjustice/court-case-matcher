package uk.gov.justice.probation.courtcasematcher.model.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum HearingEventType {
    CONFIRMED_OR_UPDATED("ConfirmedOrUpdated"),
    RESULTED("Resulted"),
    UNKNOWN("Unknown");

    private static final HearingEventType DEFAULT = CONFIRMED_OR_UPDATED;

    final String description;

    HearingEventType(String description) {
        this.description = description;
    }

    public String getDescription(){
        return description;
    }

    @JsonCreator
    public static HearingEventType of(String eventTypeDescription) {
        eventTypeDescription = eventTypeDescription == null ? DEFAULT.name() : eventTypeDescription;
        switch (eventTypeDescription) {
            case "ConfirmedOrUpdated":
                return CONFIRMED_OR_UPDATED;
            case "Resulted":
                return RESULTED;
            default:
                log.warn("Unknown hearing event type received {}. Returning CONFIRMED_OR_UPDATED.", eventTypeDescription);
                return DEFAULT;
        }
    }

    public static String from(HearingEventType eventType) {
        eventType = eventType == null ? DEFAULT : eventType;
        switch (eventType) {
            case CONFIRMED_OR_UPDATED:
                return CONFIRMED_OR_UPDATED.description;
            case RESULTED:
                return RESULTED.description;
            default:
                log.warn("Unknown hearing event type received {}. Returning CONFIRMED_OR_UPDATED.", eventType);
                return DEFAULT.description;
        }
    }
}
