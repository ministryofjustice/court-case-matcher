package uk.gov.justice.probation.courtcasematcher.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Profile("!test & !local")
public class S3Config {

    @Bean
    public S3Client amazonS3Client(@Value("${aws.region-name}") String regionName) {
        return S3Client.builder()
            .region(Region.of(regionName))
            .build();
    }

    @Bean
    public S3AsyncClient s3AsyncClient(@Value("${aws.region-name}") String regionName) {
        return S3AsyncClient.builder().region(software.amazon.awssdk.regions.Region.of(regionName)).build();
    }
}
