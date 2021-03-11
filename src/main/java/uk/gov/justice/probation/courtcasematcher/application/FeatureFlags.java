package uk.gov.justice.probation.courtcasematcher.application;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConfigurationProperties(prefix = "feature")
public class FeatureFlags {

    public static final String USE_OFFENDER_SEARCH_FOR_PROBATION_STATUS = "use-offender-search-for-probation-status";

    private final Map<String, Boolean> flags;

    public FeatureFlags() {
        this.flags = new HashMap<>();
    }

    public Map<String, Boolean> getFlags() {
        return flags;
    }

    public boolean offenderSearchForProbationStatus() {
        return flags.getOrDefault(USE_OFFENDER_SEARCH_FOR_PROBATION_STATUS, false);
    }

    public void setFlagValue(final String flagName, final boolean value) {
        flags.put(flagName, value);
    }

    public void setFlags(final Map<String, Boolean> flags) {
        this.flags.putAll(flags);
    }


    @PostConstruct
    public void init() {
        log.info("Feature flags at startup:" + flags.entrySet().toString());
    }
}
