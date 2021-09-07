package uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.DataSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CCSDataSourceTest {

    @Test
    public void mapFromDomain() {
        assertThat(CCSDataSource.of(DataSource.LIBRA)).isEqualTo(CCSDataSource.LIBRA);
        assertThat(CCSDataSource.of(DataSource.COMMON_PLATFORM)).isEqualTo(CCSDataSource.COMMON_PLATFORM);
    }

    @Test
    public void mapToDomain() {
        assertThat(CCSDataSource.COMMON_PLATFORM.asDomain()).isEqualTo(DataSource.COMMON_PLATFORM);
        assertThat(CCSDataSource.LIBRA.asDomain()).isEqualTo(DataSource.LIBRA);
    }

    @Test
    public void ofToleratesNull() {
        assertThat(CCSDataSource.of(null)).isEqualTo(CCSDataSource.LIBRA);
    }
}
