package uk.gov.justice.probation.courtcasematcher.application;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "case-mapper-reference")
public class CaseMapperReference {

    @Value("${case-mapper-reference.defaultProbationStatus}")
    private String defaultProbationStatus;

    private final Map<String, String> courtNameToCodes = new HashMap<>();

    public void setCourtNameToCodes(Map<String, String> courtNameToCodes) {
        this.courtNameToCodes.putAll(courtNameToCodes);
    }

    public void setDefaultProbationStatus(String defaultProbationStatus) {
        this.defaultProbationStatus = defaultProbationStatus;
    }

    public String getCourtCodeFromName(String courtName) {
        return courtNameToCodes.get(courtName.replaceAll("\\s+",""));
    }

    public String getDefaultProbationStatus() {
        return defaultProbationStatus;
    }
}
