package uk.gov.justice.probation.courtcasematcher.controller;

import com.amazonaws.services.s3.AmazonS3;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

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
    @Test
    void replays404Hearings() throws InterruptedException, IOException {
        // iterate over hearings.csv and upload them
        // wire in path to file
        // nullpointer on this line for some reason
        // delete them afterwards
        s3Client.putObject(bucketName, "cp/REALPATH/14-51-25-315432427-55bf2979-5b84-42b5-ac4c-58b8238df493", Paths.get("src/test/resources/replay404hearings/hearingFromS3.json").toFile());
        WebClient webClient = WebClient.builder()
            .build();
        String replay404HearingsUrl = String.format("http://localhost:%d/replay404Hearings", port);
        String OK = webClient.post().uri(URI.create(replay404HearingsUrl))
            .retrieve().bodyToMono(String.class).block();
        Thread.sleep(2000);
        assertThat(OK.equals("OK")).isTrue();
//        MOCK_SERVER.findAllUnmatchedRequests();
//        MOCK_SERVER.verify(
//            postRequestedFor(urlEqualTo(String.format("/hearing/%s", "new hearing ID")))
//        );

    }


}




