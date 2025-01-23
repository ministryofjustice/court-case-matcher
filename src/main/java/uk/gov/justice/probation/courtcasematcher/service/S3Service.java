package uk.gov.justice.probation.courtcasematcher.service;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@NoArgsConstructor
public class S3Service {

    @Value("${aws.s3.bucket_name}")
    private String bucketName;

    @Autowired
    private S3AsyncClient s3AsyncClient;

    public String getObject(String key){
        try {
          String message = s3AsyncClient.getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build(), AsyncResponseTransformer.toBytes()).get().asUtf8String();
          log.info("Successfully downloaded large s3 object {}", key);
          return message;
        } catch (RuntimeException | InterruptedException | ExecutionException e) {
          log.error("Failed to get file {} from S3", key, e);
          throw new RuntimeException("Failed to get file from S3", e);
        }
    }
}