package uk.gov.justice.probation.courtcasematcher.application;

import com.amazonaws.services.s3.AmazonS3;

import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class S3Config {

    @Bean
    public AmazonS3 amazonS3Client(@Value("${aws.region-name}")String regionName) {
        return AmazonS3ClientBuilder
            .standard()
            .withRegion(regionName)
            .build();
    }
}
