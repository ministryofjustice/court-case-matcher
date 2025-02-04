package uk.gov.justice.probation.courtcasematcher.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
@Profile("local")
public class LocalAwsConfig {

    @Value("${aws.region-name}")
    private String regionName;

    @Value("${aws.endpoint-url}")
    private String endpointUrl;

    @Bean
    public S3Client amazonS3Client() {
        return S3Client.builder()
            .endpointOverride(URI.create(endpointUrl))
            .forcePathStyle(true)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("any", "any")))
            .region(Region.of(regionName))
            .build();
    }
}
