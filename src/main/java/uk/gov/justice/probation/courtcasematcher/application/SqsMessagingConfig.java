package uk.gov.justice.probation.courtcasematcher.application;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import uk.gov.justice.probation.courtcasematcher.service.SqsService;

@Profile("!test")
@Configuration
public class SqsMessagingConfig {

    @EnableAutoConfiguration(exclude = ArtemisAutoConfiguration.class)
    private static class ArtemisAutoConfigToggle{}

    @Primary
    @Bean(name = "amazonSQSAsync")
    @Profile("sqs-ccm-messaging")
    public AmazonSQSAsync amazonSQSAsync(@Value("${aws.region-name}") final String regionName,
                                        @Value("${aws_sqs_court_case_matcher_endpoint_url}") final String awsEndpointUrl,
                                        @Value("${aws_sqs_court_case_matcher_access_key_id}") final String awsAccessKeyId,
                                        @Value("${aws_sqs_court_case_matcher_secret_access_key}") final String awsSecretAccessKey) {
        return AmazonSQSAsyncClientBuilder
            .standard()
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey)))
            .withEndpointConfiguration(new EndpointConfiguration(awsEndpointUrl, regionName))
            .build();
    }

    @Primary
    @Bean(name = "cpgAmazonSQSAsync")
    @Profile("sqs-cpg-messaging")
    public AmazonSQSAsync cpgAmazonSQSAsync(@Value("${aws.region-name}") final String regionName,
        @Value("${aws_sqs_crime_portal_gateway_endpoint_url}") final String awsEndpointUrl,
        @Value("${aws_sqs_crime_portal_gateway_access_key_id}") final String awsAccessKeyId,
        @Value("${aws_sqs_crime_portal_gateway_secret_access_key}") final String awsSecretAccessKey) {
        return AmazonSQSAsyncClientBuilder
            .standard()
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey)))
            .withEndpointConfiguration(new EndpointConfiguration(awsEndpointUrl, regionName))
            .build();
    }

    @Primary
    @Profile("sqs-cpg-messaging")
    @Bean
    public SqsService cpgSqsService(AmazonSQSAsync cpgAmazonSQSAsync, @Value("${aws.sqs.crime_portal_gateway_queue_name}") String queueName) {
        return new SqsService(queueName, cpgAmazonSQSAsync);
    }

    @Primary
    @Profile("sqs-ccm-messaging")
    @Bean
    public SqsService sqsService(AmazonSQSAsync amazonSQSAsync,  @Value("${aws.sqs.court_case_matcher_queue_name}") String queueName) {
        return new SqsService(queueName, amazonSQSAsync);
    }

}
