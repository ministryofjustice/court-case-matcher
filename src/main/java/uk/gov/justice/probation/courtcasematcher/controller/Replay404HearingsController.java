package uk.gov.justice.probation.courtcasematcher.controller;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.probation.courtcasematcher.messaging.HearingProcessor;
import uk.gov.justice.probation.courtcasematcher.messaging.MessageParser;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPHearingEvent;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseServiceClient;
import uk.gov.justice.probation.courtcasematcher.service.ReplayHearingsService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryEventType;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
@Slf4j
public class Replay404HearingsController {

    private final ReplayHearingsService replayHearingsService;

    Replay404HearingsController(ReplayHearingsService replayHearingsService){
        this.replayHearingsService = replayHearingsService;
    }

    @PostMapping("/replay404Hearings")
    public String replay404Hearings() {
        // put some proper exception handling in place

        // check how long thread timeout is
        log.info("Starting to replay 404 hearings");
        CompletableFuture.supplyAsync(replayHearingsService.replay());

        // How to test end-2-end
        // put a few updated hearings (minor updates) in dev/preprod to the S3 bucket
        // call this endpoint to pick up on those unprocessed hearings in the s3 bucket and process them

        // dry run option?

        return "OK";
    }
}
