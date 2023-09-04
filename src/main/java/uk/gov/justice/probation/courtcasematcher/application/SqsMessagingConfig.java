package uk.gov.justice.probation.courtcasematcher.application;

import com.amazonaws.client.builder.AwsClientBuilder;
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
                                                   @Value("${aws.sqs.court_case_matcher_endpoint_url}") final String awsEndpointUrl) {
        return getAmazonSQSAsync(awsEndpointUrl, regionName);
    }

    @Bean(name = "courtCaseMatcherSqsDlq")
    public AmazonSQSAsync courtCaseMatcherSqsDlq(@Value("${aws.region-name}") final String regionName,
                                                 @Value("${aws.sqs.court_case_matcher_dlq_endpoint_url}") final String awsEndpointUrl)
    {
        return getAmazonSQSAsync(awsEndpointUrl, regionName);
    }

    private AmazonSQSAsync getAmazonSQSAsync(String awsEndpointUrl, String regionName) {
        return AmazonSQSAsyncClientBuilder
                .standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsEndpointUrl, regionName))
                .build();
    }

    @Primary
    @Bean
    public SqsService sqsService(AmazonSQSAsync courtCaseMatcherSqsQueue, @Value("${aws.sqs.court_case_matcher_queue_name}") String queueName) {
        return new SqsService(queueName, courtCaseMatcherSqsQueue);
    }

}
