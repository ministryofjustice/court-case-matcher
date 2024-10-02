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

import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
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
        MOCK_SERVER.addMockServiceRequestListener(
            Replay404HearingsControllerIntTest::requestReceived);
        for (String hearing : Files.readAllLines(Paths.get(pathToCsv), UTF_8)) {
            String[] hearingDetails = hearing.split(",");
            String id = hearingDetails[0];
            String s3Path = hearingDetails[1];
            // might need to get this file, replace the ID on line 25 with the actual ID
            s3Client.putObject(bucketName, s3Path, Files.readString(Paths.get("src/test/resources/replay404hearings/hearingFromS3.json")).replace("|REPLACEMEID|", id));
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
        MOCK_SERVER.verify(
            putRequestedFor(urlEqualTo("/hearing/8bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f"))
        );
        MOCK_SERVER.verify(
            putRequestedFor(urlEqualTo("/hearing/NEWHEARINGf0b1b82c-9728-4ab0-baca-b744c50ba9c8"))
        );
        MOCK_SERVER.verify(
            0,
            putRequestedFor(urlEqualTo("/hearing/UPDATEDSINCE404HEARINGd11ee8c1-7526-4509-9579-b253868943d9"))
        );
    }
    protected static void requestReceived(com.github.tomakehurst.wiremock.http.Request inRequest,
                                          com.github.tomakehurst.wiremock.http.Response inResponse) {
        System.out.printf("WireMock request at URL: %s", inRequest.getAbsoluteUrl());
        System.out.printf("WireMock request body: \n%s", inRequest.getBodyAsString());
        System.out.printf("WireMock response body: \n%s", inResponse.getBodyAsString());
        System.out.printf("WireMock response headers: \n%s", inResponse.getHeaders());
    }

}




