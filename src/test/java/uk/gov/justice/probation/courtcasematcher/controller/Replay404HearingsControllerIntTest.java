package uk.gov.justice.probation.courtcasematcher.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test"})
@Import(TestMessagingConfig.class)
@DirtiesContext
public class Replay404HearingsControllerIntTest {

    @LocalServerPort
    protected int port;

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @Test
    void replays404Hearings() throws InterruptedException {

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


