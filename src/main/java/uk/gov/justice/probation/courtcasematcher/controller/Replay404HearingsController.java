package uk.gov.justice.probation.courtcasematcher.controller;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.probation.courtcasematcher.messaging.HearingProcessor;
import uk.gov.justice.probation.courtcasematcher.messaging.MessageParser;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPHearingEvent;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseServiceClient;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
@Slf4j
public class Replay404HearingsController {

    private final String pathToCsv;
    private final CourtCaseServiceClient courtCaseServiceClient;
    private final AmazonS3 s3Client;
    private final String bucketName;
    private final MessageParser<CPHearingEvent> commonPlatformParser;
    private final HearingProcessor hearingProcessor;
    private final boolean dryRunEnabled;

    public Replay404HearingsController(@Value("${replay404.path-to-csv}") String pathToCsv,
                                       CourtCaseServiceClient courtCaseServiceClient, AmazonS3 s3Client,
                                       @Value("${crime-portal-gateway-s3-bucket}") String bucketName,
                                       final MessageParser<CPHearingEvent> commonPlatformParser,
                                       final HearingProcessor hearingProcessor,
                                       @Value("${replay404.dry-run}") boolean dryRunEnabled)
    {
        this.pathToCsv = pathToCsv;
        this.courtCaseServiceClient = courtCaseServiceClient;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.commonPlatformParser = commonPlatformParser;
        this.hearingProcessor = hearingProcessor;
        this.dryRunEnabled = dryRunEnabled;
    }

    @PostMapping("/replay404Hearings")
    public String replay404Hearings() {
        // put some proper exception handling in place

        // check how long thread timeout is
        log.info("Starting to replay 404 hearings");
        CompletableFuture.supplyAsync(replay404s());

        // How to test end-2-end
        // put a few updated hearings (minor updates) in dev/preprod to the S3 bucket
        // call this endpoint to pick up on those unprocessed hearings in the s3 bucket and process them

        // dry run option?

        return "OK";
    }

    @NotNull
    private Supplier<String> replay404s() {
        return () -> {
            try {
                List<String> hearingsWith404 = Files.readAllLines(Paths.get(pathToCsv), UTF_8);
                int numberToProcess = hearingsWith404.size();
                log.info("Total number of hearings {}", numberToProcess);
                int count = 0;
                for (String hearing : hearingsWith404) {

                    String[] hearingDetails = hearing.split(",");
                    String id = hearingDetails[0];
                    String s3Path = hearingDetails[1];
                    LocalDateTime received = LocalDateTime.from(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").parse(hearingDetails[2])); // THIS IS UTC and therefore 1 hour behind the time in the S3 path

                    courtCaseServiceClient.getHearing(id).blockOptional().ifPresentOrElse(
                        (existingHearing) -> {
                            // check court-case-service and compare the last updated date with the inputted-hearing
                            if (existingHearing.getLastUpdated().isBefore(received)) {
                                System.out.println("Processing hearing " + id + " as it has not been updated since " + existingHearing.getLastUpdated());
                                processNewOrUpdatedHearing(s3Path);
                            } else {
                                System.out.println("Discarding hearing " + id + " as we have a later version of it on " + existingHearing.getLastUpdated());
                            }
                        },
                        () -> {
                            System.out.println("Processing hearing " + id + " as it is new");
                            processNewOrUpdatedHearing(s3Path);
                        }
                    );
                    log.info("Processed hearing number {} of {}", ++count, numberToProcess);
                }
                log.info("Processing complete. {} of {} processed",count,numberToProcess);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                throw new RuntimeException(e);
            }

            return "Finished!";
        };
    }

    private void processNewOrUpdatedHearing(String s3Path) {

        String message  = s3Client.getObjectAsString(bucketName, s3Path);
        CPHearingEvent cpHearingEvent;
        try {
            cpHearingEvent = commonPlatformParser.parseMessage(message, CPHearingEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        // might need to import the hearing classes from CHER if this is too different
        final var hearing = cpHearingEvent.asDomain()
            .withHearingId(cpHearingEvent.getHearing().getId())
            .withHearingEventType("CONFIRM_UPDATE");

        // mark hearing as processed somehow?
        // tag S3 object?
        // dry run part here
            if (dryRunEnabled) {
                log.info("Dry run - processNewOrUpdatedHearing for hearing: {}", cpHearingEvent.getHearing().getId());
            } else {
                hearingProcessor.process(hearing, "some-id"); //TODO see if this can be retrieved. It is only used for logging
            }
    }
}
