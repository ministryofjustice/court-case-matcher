package uk.gov.justice.probation.courtcasematcher.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;


public class Replay404HearingsControllerIntTest extends Replay404HearingsControllerIntTestBase{

    @Test
    void replays404Hearings() throws InterruptedException, IOException {
        String OK = replayHearings();
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

    }

}
