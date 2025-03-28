package uk.gov.justice.probation.courtcasematcher.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.application.FeatureFlags;

import java.util.List;

@Service
@Slf4j
public class CprExtractor {

    public static final String OXFORD_MAGISTRATES = "B43KB";

    private final List<String> courtCodes = List.of(OXFORD_MAGISTRATES);
    private final FeatureFlags featureFlags;

    public CprExtractor(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    public boolean canExtractCprFields(String courtCode) {
        if(featureFlags.getFlag("cpr_matcher")) {
            return courtCodes.contains(courtCode);
        }
        return false;
    }
}
