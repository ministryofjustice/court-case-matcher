package uk.gov.justice.probation.courtcasematcher.application;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@ConfigurationProperties(prefix = "feature")
public class FeatureFlags {

    @NonNull
    @Getter
    private final Map<String, Boolean> flags;

    public FeatureFlags() {
        this.flags = new HashMap<>();
    }


    public void setFlagValue(final String flagName, final boolean value) {
        flags.put(flagName, value);
    }

    public void setFlags(final Map<String, Boolean> flags) {
        this.flags.putAll(flags);
    }


    @PostConstruct
    public void init() {
        log.info("Feature flags at startup:" + flags.entrySet());
    }
}
