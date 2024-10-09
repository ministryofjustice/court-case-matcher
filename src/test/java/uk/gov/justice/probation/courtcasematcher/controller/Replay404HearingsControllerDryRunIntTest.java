package uk.gov.justice.probation.courtcasematcher.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import uk.gov.justice.probation.courtcasematcher.service.Replay404HearingProcessStatus;

import java.io.IOException;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

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

        Map<String, String> firstHearing = Map.of(
            "hearingId", "8bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f",
            "status", Replay404HearingProcessStatus.SUCCEEDED.status,
            "dryRun","true");
        verify(telemetryService).track404HearingProcessedEvent(firstHearing);

        Map<String, String> secondHearing = Map.of(
            "hearingId", "d11ee8c1-7526-4509-9579-b253868943d9",
            "status", Replay404HearingProcessStatus.OUTDATED.status,
            "dryRun","true");
        verify(telemetryService).track404HearingProcessedEvent(secondHearing);

        Map<String, String> thirdHearing = Map.of(
            "hearingId", "f0b1b82c-9728-4ab0-baca-b744c50ba9c8",
            "status", Replay404HearingProcessStatus.SUCCEEDED.status,
            "dryRun","true");
        verify(telemetryService).track404HearingProcessedEvent(thirdHearing);
    }

}
