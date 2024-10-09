package uk.gov.justice.probation.courtcasematcher.controller;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.service.Replay404HearingProcessStatus;

import java.io.IOException;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;


public class Replay404HearingsControllerIntTest extends Replay404HearingsControllerIntTestBase{

    @Test
    void replays404HearingsWhichCanBeProcessed() throws InterruptedException, IOException {
        String OK = replayHearings(hearingsWhichCanBeProcessed);
        Thread.sleep(2000);

        MOCK_SERVER.verify(
            putRequestedFor(urlEqualTo("/hearing/8bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f"))
                .withRequestBody(matchingJsonPath("caseId", equalTo("D517D32D-3C80-41E8-846E-D274DC2B94A5")))
                // ??? CAUSE FOR CONCERN HERE, LOOKS TO BE PRESENT .withRequestBody(matchingJsonPath("hearingType", equalTo("sentence")))
                .withRequestBody(matchingJsonPath("hearingId", equalTo("8bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f")))
                .withRequestBody(matchingJsonPath("hearingEventType", equalTo("ConfirmedOrUpdated")))
                .withRequestBody(matchingJsonPath("caseNo", equalTo("D517D32D-3C80-41E8-846E-D274DC2B94A5")))
                .withRequestBody(matchingJsonPath("hearingDays[0].courtCode", equalTo("B10JQ")))
                .withRequestBody(matchingJsonPath("defendants[0].defendantId", equalTo("ac24a1be-939b-49a4-a524-21a3d228f8bc")))
                .withRequestBody(matchingJsonPath("defendants[0].pnc", equalTo("2004/0000500U")))

                // TODO mock and test Values from offender search - see other tests for details

                // TODO mock and test Values from probation search (if relevant)
        );

        MOCK_SERVER.verify(
            0,
            putRequestedFor(urlEqualTo("/hearing/d11ee8c1-7526-4509-9579-b253868943d9"))
        );
        MOCK_SERVER.verify(
            putRequestedFor(urlEqualTo("/hearing/f0b1b82c-9728-4ab0-baca-b744c50ba9c8"))
        );
        assertThat(OK).isEqualTo("OK");
        Map<String, String> firstHearing = Map.of(
        "hearingId", "8bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f",
        "status", Replay404HearingProcessStatus.SUCCEEDED.status,
        "dryRun","false");
        verify(telemetryService).track404HearingProcessedEvent(firstHearing);

        Map<String, String> secondHearing = Map.of(
            "hearingId", "d11ee8c1-7526-4509-9579-b253868943d9",
            "status", Replay404HearingProcessStatus.OUTDATED.status,
            "dryRun","false");
         verify(telemetryService).track404HearingProcessedEvent(secondHearing);

        Map<String, String> thirdHearing = Map.of(
            "hearingId", "f0b1b82c-9728-4ab0-baca-b744c50ba9c8",
            "status", Replay404HearingProcessStatus.SUCCEEDED.status,
            "dryRun","false");
         verify(telemetryService).track404HearingProcessedEvent(thirdHearing);
    }

    @Test
    void replays404HearingsWithNoProsecutionCasesWhichCannotBeProcessed() throws InterruptedException, IOException {
        String OK = replayHearings(hearingsWithNoProsecutionCases);
        Thread.sleep(2000);

        MOCK_SERVER.verify(
            0,
            putRequestedFor(urlEqualTo("/hearing/1bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f"))
        );
        MOCK_SERVER.verify(
            0,
            putRequestedFor(urlEqualTo("/hearing/e0b1b82c-9728-4ab0-baca-b744c50ba9c8"))
        );
        assertThat(OK).isEqualTo("OK");

        Map<String, String> firstHearing = Map.of(
            "hearingId", "1bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f",
            "status", Replay404HearingProcessStatus.INVALID.status,
            "dryRun","false",
            "reason", "hearing.prosecutionCases: must not be empty"
        );
        verify(telemetryService).track404HearingProcessedEvent(firstHearing);

        Map<String, String> secondHearing = Map.of(
            "hearingId", "e0b1b82c-9728-4ab0-baca-b744c50ba9c8",
            "status", Replay404HearingProcessStatus.INVALID.status,
            "dryRun","false",
            "reason", "hearing.prosecutionCases: must not be empty"
        );
        verify(telemetryService).track404HearingProcessedEvent(secondHearing);
    }

    @Test
    void replays404HearingsWithNoCaseUrnWhichCannotBeProcessed() throws InterruptedException, IOException {
        String OK = replayHearings(hearingsWithNoCaseUrns);
        Thread.sleep(2000);

        MOCK_SERVER.verify(
            0,
            putRequestedFor(urlEqualTo("/hearing/9bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f"))
        );
        MOCK_SERVER.verify(
            0,
            putRequestedFor(urlEqualTo("/hearing/d0b1b82c-9728-4ab0-baca-b744c50ba9c8"))
        );
        assertThat(OK).isEqualTo("OK");

        Map<String, String> firstHearing = Map.of(
            "hearingId", "9bbb4fe3-a899-45c7-bdd4-4ee25ac5a83f",
            "status", Replay404HearingProcessStatus.INVALID.status,
            "dryRun","false",
            "reason", "hearing.prosecutionCases[0].prosecutionCaseIdentifier.caseUrn: must not be blank"
        );
        verify(telemetryService).track404HearingProcessedEvent(firstHearing);

        Map<String, String> secondHearing = Map.of(
            "hearingId", "d0b1b82c-9728-4ab0-baca-b744c50ba9c8",
            "status", Replay404HearingProcessStatus.INVALID.status,
            "dryRun","false",
            "reason", "hearing.prosecutionCases[0].prosecutionCaseIdentifier.caseUrn: must not be blank"
        );
        verify(telemetryService).track404HearingProcessedEvent(secondHearing);
    }

    @Test
    void replays404HearingsWhichThrowErrorsWhenProcessing() throws InterruptedException, IOException {
        doThrow(new RuntimeException("fake exception")).when(telemetryService).trackNewHearingEvent(any(), anyString());
        String OK = replayHearings(hearingsWhichCanBeProcessed);
        Thread.sleep(2000);
        verify(telemetryService).trackNewHearingEvent(any(), anyString());

        assertThat(OK).isEqualTo("OK");

        Map<String, String> thirdHearing = Map.of(
            "hearingId", "f0b1b82c-9728-4ab0-baca-b744c50ba9c8",
            "status", Replay404HearingProcessStatus.FAILED.status,
            "reason", "fake exception",
            "dryRun","false");
        verify(telemetryService).track404HearingProcessedEvent(thirdHearing);
    }

}
