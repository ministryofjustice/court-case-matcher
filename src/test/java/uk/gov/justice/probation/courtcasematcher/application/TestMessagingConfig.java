package uk.gov.justice.probation.courtcasematcher.application;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestMessagingConfig {
    @Value("${aws.region-name}")
    String regionName;
    @Value("http://localhost:4566")
    String endpointUrl;

    @Bean
    public AmazonS3 amazonS3()  {
        EndpointConfiguration endpointConfiguration = new EndpointConfiguration(endpointUrl, regionName);

        return AmazonS3ClientBuilder
            .standard()
            .withPathStyleAccessEnabled(true)
            .withEndpointConfiguration(endpointConfiguration)
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("any", "any")))
            .build();
    }

    @MockBean
    private BuildProperties buildProperties;


}
