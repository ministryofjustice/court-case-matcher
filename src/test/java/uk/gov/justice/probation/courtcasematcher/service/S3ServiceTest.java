package uk.gov.justice.probation.courtcasematcher.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class S3ServiceTest {

    @Mock
    S3Client s3Client;

    @InjectMocks
    S3Service s3Service;

    @Test
    void givenLargeMessageExistsInS3_thenGetTheMessageFromS3(){
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenReturn(ResponseBytes.fromInputStream(
                GetObjectResponse.builder()
                    .acceptRanges("bytes")
                    .lastModified(null)
                    .contentLength(268444L)
                    .eTag("440b3a55a7f9e2f2b6c7cc47a1011016")
                    .contentType("text/plain")
                    .serverSideEncryption("AES256")
                    .build(),
                new ByteArrayInputStream("Hello World".getBytes()
            )));

        assertThat(s3Service.getObject("")).isEqualTo("Hello World");
    }

    @Test
    void givenLargeMessageDoesNotExistsInS3_thenGetTheMessageFromS3(){
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenThrow(new RuntimeException("Failed to get file from S3"));

        assertThrows(RuntimeException.class, () -> s3Service.getObject(""));
    }
}
