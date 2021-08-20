package uk.gov.justice.probation.courtcasematcher.restclient.model;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum DefendantType {
    ORGANISATION("O"),
    PERSON("P")
    ;

    private static final DefendantType DEFAULT = PERSON;

    final String type;

    DefendantType(String type) {
        this.type = type;
    }

    public static DefendantType of(uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType defType) {
        if(defType == null) {
            return DEFAULT;
        }
        switch (defType) {
            case ORGANISATION:
                return ORGANISATION;
            case PERSON:
                return PERSON;
            default:
                // This shouldn't happen unless domain DefendantType changes
                log.warn("Unknown defendant type received {}. Returning PERSON.", defType);
                return DEFAULT;
        }
    }
}
