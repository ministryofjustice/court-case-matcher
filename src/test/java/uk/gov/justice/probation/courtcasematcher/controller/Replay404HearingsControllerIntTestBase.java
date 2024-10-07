package uk.gov.justice.probation.courtcasematcher.controller;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test"})
@Import({TestMessagingConfig.class})
@DirtiesContext
public class Replay404HearingsControllerIntTestBase {

    @LocalServerPort
    protected int port;

    protected static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @Autowired
    private AmazonS3 s3Client;

    @Value("${crime-portal-gateway-s3-bucket}")
    private String bucketName;

    @Value("${replay404.path-to-csv}")
    protected String pathToCsv;

    @BeforeEach
    void setUp() throws IOException {
        boolean logWiremock = false;
        if (logWiremock) {
            MOCK_SERVER.addMockServiceRequestListener(Replay404HearingsControllerIntTestBase::requestReceived);
        }
        for (String hearing : Files.readAllLines(Paths.get(pathToCsv), UTF_8)) {
            String[] hearingDetails = hearing.split(",");
            String id = hearingDetails[0];
            String s3Path = hearingDetails[1];

            String fileWithCorrectId = Files.readString(Paths.get("src/test/resources/replay404hearings/hearingFromS3.json")).replace("|REPLACEMEID|", id);
            s3Client.putObject(bucketName, s3Path, fileWithCorrectId);
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

    protected String replayHearings() throws IOException {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", Files.readAllBytes(Paths.get(pathToCsv))).header("Content-Disposition", "form-data; name=file; filename=file");
        WebClient webClient = WebClient.builder()
            .build();
        String replay404HearingsUrl = String.format("http://localhost:%d/replay404Hearings", port);
        String OK = webClient.post()
            .uri(URI.create(replay404HearingsUrl))
            .contentType(MediaType.parseMediaType("multipart/form-data; boundary=------------------------WVYuKPhkvydQGkHSFHmKE2"))
            .body(BodyInserters.fromMultipartData(builder.build()))
            .retrieve().bodyToMono(String.class).block();
        return OK;
    }


    protected static void requestReceived(com.github.tomakehurst.wiremock.http.Request inRequest, com.github.tomakehurst.wiremock.http.Response inResponse) {
        System.out.printf("WireMock request at URL: %s", inRequest.getAbsoluteUrl());
        System.out.printf("WireMock request body: \n%s", inRequest.getBodyAsString());
        System.out.printf("WireMock response body: \n%s", inResponse.getBodyAsString());
        System.out.printf("WireMock response headers: \n%s", inResponse.getHeaders());
    }

}
