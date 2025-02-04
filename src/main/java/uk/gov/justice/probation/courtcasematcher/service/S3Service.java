package uk.gov.justice.probation.courtcasematcher.service;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@NoArgsConstructor
public class S3Service {

    @Value("${aws.s3.large-hearings.bucket-name}")
    private String bucketName;

    @Autowired
    private S3Client s3Client;

    public String getObject(String key){
        try {
          String message = s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucketName).key(key).build()).asString(StandardCharsets.UTF_8);
          log.info("Successfully downloaded large s3 object {}", key);
          return message;
        } catch (RuntimeException e) {
          log.error("Failed to get file {} from S3", key, e);
          throw new RuntimeException("Failed to get file from S3", e);
        }
    }
}