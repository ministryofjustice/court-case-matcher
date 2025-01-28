package uk.gov.justice.probation.courtcasematcher.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@TestConfiguration
public class TestMessagingConfig {
    @Value("${aws.region-name}")
    String regionName;
    @Value("http://localhost:4566")
    String endpointUrl;

    @Bean
    public S3Client amazonS3LocalStackClient() {
        return S3Client.builder()
            .endpointOverride(URI.create(endpointUrl))
            .forcePathStyle(true)
            .region(Region.of(regionName))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("any", "any")))
            .build();
    }
}
