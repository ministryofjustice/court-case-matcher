package uk.gov.justice.probation.courtcasematcher.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"replay404.dry-run = true"})
public class Replay404HearingsControllerDryRunIntTest extends Replay404HearingsControllerIntTest {
    @Test
    void givenDryRunEnabled_then_replay_404Hearings() throws InterruptedException, IOException {

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
        Thread.sleep(2000);

        // TODO maybe some stronger checks about the format of the put body here?
        MOCK_SERVER.verify(
            0,
            putRequestedFor(urlEqualTo("/hearing/8bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f"))
        );
        MOCK_SERVER.verify(
            0,
            putRequestedFor(urlEqualTo("/hearing/d11ee8c1-7526-4509-9579-b253868943d9"))
        );
        MOCK_SERVER.verify(
            0,
            putRequestedFor(urlEqualTo("/hearing/f0b1b82c-9728-4ab0-baca-b744c50ba9c8"))
        );
        assertThat(OK).isEqualTo("OK");

    }

}
