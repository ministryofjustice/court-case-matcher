package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;

import java.util.Optional;

@Slf4j
public enum CCSDataSource {
    LIBRA(DataSource.LIBRA),
    COMMON_PLATFORM(DataSource.COMMON_PLATFORM);

    private final DataSource domainValue;

    CCSDataSource(DataSource domain) {
        domainValue = domain;
    }

    public DataSource asDomain() {
        return domainValue;
    }

    public static CCSDataSource of(DataSource source) {
        switch (Optional.ofNullable(source).orElse(DataSource.LIBRA)) {
            case COMMON_PLATFORM:
                return COMMON_PLATFORM;
            case LIBRA:
                return LIBRA;
            default:
                log.warn(String.format("Unexpected DataSource %s, defaulting to LIBRA", source));
                return LIBRA;
        }
    }
}
