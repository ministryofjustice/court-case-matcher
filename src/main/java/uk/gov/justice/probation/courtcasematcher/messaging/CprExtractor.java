package uk.gov.justice.probation.courtcasematcher.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.application.FeatureFlags;

import java.util.List;

@Service
@Slf4j
public class CprExtractor {

    public static final String BASINGSTOKE = "B44BA";
    public static final String MILTON_KEYNES = "B43JC";
    public static final String NEWPORT_ISLE_OF_WIGHT = "B44JK";
    public static final String OXFORD = "B43KB";
    public static final String PORTSMOUTH = "B44KM";
    public static final String READING = "B43KQ";
    public static final String SOUTHAMPTON = "B44MA";
    public static final String HIGH_WYCOMBE = "B43OX";

    private final List<String> courtCodes = List.of(BASINGSTOKE, MILTON_KEYNES,NEWPORT_ISLE_OF_WIGHT,
        OXFORD, PORTSMOUTH, READING, SOUTHAMPTON, HIGH_WYCOMBE);
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
