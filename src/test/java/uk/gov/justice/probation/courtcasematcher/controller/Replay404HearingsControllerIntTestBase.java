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

    protected String hearingsWhichCanBeProcessed = "src/test/resources/replay404hearings/test-hearings.csv";
    protected String hearingsWithNoProsecutionCases = "src/test/resources/replay404hearings/test-hearings-with-no-prosecution-cases.csv";
    protected String hearingsWithNoCaseUrns = "src/test/resources/replay404hearings/test-hearings-with-no-case-urns.csv";
    @BeforeEach
    void setUp() throws IOException {
        boolean logWiremock = false;
        if (logWiremock) {
            MOCK_SERVER.addMockServiceRequestListener(Replay404HearingsControllerIntTestBase::requestReceived);
        }
        publishToS3(hearingsWhichCanBeProcessed, "src/test/resources/replay404hearings/hearingFromS3.json");
        publishToS3(hearingsWithNoProsecutionCases, "src/test/resources/replay404hearings/hearingWithNoProsecutionCases.json");
        publishToS3(hearingsWithNoCaseUrns, "src/test/resources/replay404hearings/hearingWithNoCaseUrn.json");
    }

    private void publishToS3(String pathToHearings, String hearingTemplate) throws IOException {
        Files.readAllLines(Paths.get(pathToHearings), UTF_8).stream().filter(it -> !it.isEmpty()).forEach(hearing -> {
            String[] hearingDetails = hearing.split(",");
            String id = hearingDetails[0];
            String s3Path = hearingDetails[1];
            try {
                String fileWithCorrectId = Files.readString(Paths.get(hearingTemplate)).replace("|REPLACEMEID|", id);
                s3Client.putObject(bucketName, s3Path, fileWithCorrectId);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @AfterEach
    void tearDown() throws IOException {
        deleteFromS3(hearingsWhichCanBeProcessed);
        deleteFromS3(hearingsWithNoProsecutionCases);
        deleteFromS3(hearingsWithNoCaseUrns);
    }

    private void deleteFromS3(String pathToHearings) throws IOException {
        Files.readAllLines(Paths.get(pathToHearings), UTF_8).stream().filter(it -> !it.isEmpty()).forEach(hearing -> {
            String[] hearingDetails = hearing.split(",");
            String s3Path = hearingDetails[1];
            s3Client.deleteObject(bucketName, s3Path);
        });
    }

    protected String replayHearings(String pathToHearings) throws IOException {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", Files.readAllBytes(Paths.get(pathToHearings))).header("Content-Disposition", "form-data; name=file; filename=file");
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
