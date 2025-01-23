package uk.gov.justice.probation.courtcasematcher.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class S3ServiceTest {

    @Mock
    S3AsyncClient s3AsyncClient;

    @InjectMocks
    S3Service s3Service;

    @Test
    void givenLargeMessageExistsInS3_thenGetTheMessageFromS3(){
        when(s3AsyncClient.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(CompletableFuture.completedFuture(ResponseBytes.fromInputStream(
                GetObjectResponse.builder()
                    .acceptRanges("bytes")
                    .lastModified(null)
                    .contentLength(268444L)
                    .eTag("440b3a55a7f9e2f2b6c7cc47a1011016")
                    .contentType("text/plain")
                    .serverSideEncryption("AES256")
                    .build(),
                new ByteArrayInputStream("Hello World".getBytes()
            ))));

        assertThat(s3Service.getObject("")).isEqualTo("Hello World");
    }

    @Test
    void givenLargeMessageDoesNotExistsInS3_thenGetTheMessageFromS3(){
        when(s3AsyncClient.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Failed to get file from S3")));

        assertThrows(RuntimeException.class, () -> s3Service.getObject(""));
    }
}
