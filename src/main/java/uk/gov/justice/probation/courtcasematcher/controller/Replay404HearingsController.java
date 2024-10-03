package uk.gov.justice.probation.courtcasematcher.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.probation.courtcasematcher.service.ReplayHearingsService;

import java.util.concurrent.CompletableFuture;

@RestController
@Slf4j
public class Replay404HearingsController {

    private final ReplayHearingsService replayHearingsService;

    Replay404HearingsController(ReplayHearingsService replayHearingsService){
        this.replayHearingsService = replayHearingsService;
    }

    @PostMapping("/replay404Hearings")
    public String replay404Hearings() {
        log.info("Starting to replay 404 hearings");
        CompletableFuture.supplyAsync(replayHearingsService.replay());

        return "OK";
    }
}
