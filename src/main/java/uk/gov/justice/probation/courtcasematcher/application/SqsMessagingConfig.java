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
    @Bean(name = "courtCaseMatcherSqsQueue")
    public AmazonSQSAsync courtCaseMatcherSqsQueue(@Value("${aws.region-name}") final String regionName,
                                                   @Value("${aws.sqs.court_case_matcher_endpoint_url}") final String awsEndpointUrl,
                                                   @Value("${aws.sqs.court_case_matcher_access_key_id}") final String awsAccessKeyId,
                                                   @Value("${aws.sqs.court_case_matcher_secret_access_key}") final String awsSecretAccessKey) {
        return getAmazonSQSAsync(awsAccessKeyId, awsSecretAccessKey, awsEndpointUrl, regionName);
    }

    @Bean(name = "courtCaseMatcherSqsDlq")
    public AmazonSQSAsync courtCaseMatcherSqsDlq(@Value("${aws.region-name}") final String regionName,
                                                 @Value("${aws.sqs.court_case_matcher_dlq_endpoint_url}") final String awsEndpointUrl,
                                                 @Value("${aws.sqs.court_case_matcher_dlq_access_key_id}") final String awsAccessKeyId,
                                                 @Value("${aws.sqs.court_case_matcher_dlq_secret_access_key}") final String awsSecretAccessKey) {
        return getAmazonSQSAsync(awsAccessKeyId, awsSecretAccessKey, awsEndpointUrl, regionName);
    }

    private AmazonSQSAsync getAmazonSQSAsync(String awsAccessKeyId, String awsSecretAccessKey, String awsEndpointUrl, String regionName) {
        return AmazonSQSAsyncClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey)))
                .withEndpointConfiguration(new EndpointConfiguration(awsEndpointUrl, regionName))
                .build();
    }

    @Primary
    @Bean
    public SqsService sqsService(AmazonSQSAsync courtCaseMatcherSqsQueue, @Value("${aws.sqs.court_case_matcher_queue_name}") String queueName) {
        return new SqsService(queueName, courtCaseMatcherSqsQueue);
    }

}
