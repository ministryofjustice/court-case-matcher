package uk.gov.justice.probation.courtcasematcher.model.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum HearingEventType {
    CONFIRMED_OR_UPDATED("ConfirmedOrUpdated"),
    RESULTED("Resulted");

    private static final HearingEventType DEFAULT = CONFIRMED_OR_UPDATED;

    @JsonValue
    final String description;

    HearingEventType(String description) {
        this.description = description;
    }

    public String getDescription(){
        return description;
    }

    @JsonCreator
    public static HearingEventType of(@JsonProperty("Value") String eventTypeDescription) {
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
}
