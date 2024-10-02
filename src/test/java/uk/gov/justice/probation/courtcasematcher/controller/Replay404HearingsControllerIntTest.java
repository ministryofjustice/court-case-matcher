package uk.gov.justice.probation.courtcasematcher.controller;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test"})
@Import({TestMessagingConfig.class})
@DirtiesContext
public class Replay404HearingsControllerIntTest {

    @LocalServerPort
    protected int port;

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @Autowired
    private AmazonS3 s3Client;

    @Value("${crime-portal-gateway-s3-bucket}")
    private String bucketName;

    @Value("${replay404.path-to-csv}")
    private String pathToCsv;

    @BeforeEach
    void setUp() throws IOException {
        for (String hearing : Files.readAllLines(Paths.get(pathToCsv), UTF_8)) {
            String[] hearingDetails = hearing.split(",");
            String s3Path = hearingDetails[1];
            s3Client.putObject(bucketName, s3Path, Paths.get("src/test/resources/replay404hearings/hearingFromS3.json").toFile());
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        for (String hearing : Files.readAllLines(Paths.get(pathToCsv), UTF_8)) {
            String[] hearingDetails = hearing.split(",");
            String s3Path = hearingDetails[1];
            s3Client.deleteObject(bucketName, s3Path);
        }
    }
    @Test
    void replays404Hearings() throws InterruptedException {

        WebClient webClient = WebClient.builder()
            .build();
        String replay404HearingsUrl = String.format("http://localhost:%d/replay404Hearings", port);
        String OK = webClient.post().uri(URI.create(replay404HearingsUrl))
            .retrieve().bodyToMono(String.class).block();
        Thread.sleep(2000);
        assertThat(OK.equals("OK")).isTrue();
        // proper assertions here that put was called twice for new and updated records and not for the other one
//        MOCK_SERVER.findAllUnmatchedRequests();
//        MOCK_SERVER.verify(
//            postRequestedFor(urlEqualTo(String.format("/hearing/%s", "new hearing ID")))
//        );

    }


}




