package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;
import uk.gov.justice.probation.courtcasematcher.model.type.HearingEventType;

import java.util.Optional;
@Slf4j
public enum CCSHearingEventType {
    CONFIRMED_OR_UPDATED(HearingEventType.CONFIRMED_OR_UPDATED),
    RESULTED(HearingEventType.RESULTED),
    UNKNOWN(HearingEventType.UNKNOWN);

    private final HearingEventType hearingEventType;

    CCSHearingEventType(HearingEventType hearingEventType) {
        this.hearingEventType = hearingEventType;
    }

    public HearingEventType asDomain() {
        return hearingEventType;
    }

    public static CCSHearingEventType of(HearingEventType hearingEventType) {
        switch (Optional.ofNullable(hearingEventType).orElse(HearingEventType.UNKNOWN)) {
            case CONFIRMED_OR_UPDATED:
                return CONFIRMED_OR_UPDATED;
            case RESULTED:
                return RESULTED;
            default:
                log.warn(String.format("Unexpected hearting event type %s, defaulting to UNKNOWN", hearingEventType));
                return UNKNOWN;
        }
    }
}
