package uk.gov.justice.probation.courtcasematcher.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"replay404.dry-run = true"})
public class Replay404HearingsControllerDryRunIntTest extends Replay404HearingsControllerIntTestBase {
    @Test
    void givenDryRunEnabled_then_replay_404Hearings() throws InterruptedException, IOException {

        String OK = replayHearings(hearingsWhichCanBeProcessed);
        Thread.sleep(2000);

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
