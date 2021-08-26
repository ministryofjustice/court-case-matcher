package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType;

@Slf4j
public enum CCSDefendantType {
    ORGANISATION("O", DefendantType.ORGANISATION),
    PERSON("P", DefendantType.PERSON)
    ;

    private static final CCSDefendantType DEFAULT = PERSON;

    final String type;

    final DefendantType domainType;

    CCSDefendantType(String type, DefendantType domainType) {
        this.type = type;
        this.domainType = domainType;
    }

    public static CCSDefendantType of(uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType defType) {
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

    public DefendantType asDomain() {
        return domainType;
    }
}
