package uk.gov.justice.probation.courtcasematcher.service;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.controller.Hearing404;
import uk.gov.justice.probation.courtcasematcher.messaging.HearingProcessor;
import uk.gov.justice.probation.courtcasematcher.messaging.MessageParser;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPHearingEvent;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseServiceClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Service
public class ReplayHearingsService {


    private final CourtCaseServiceClient courtCaseServiceClient;
    private final AmazonS3 s3Client;
    private final String bucketName;
    private final MessageParser<CPHearingEvent> commonPlatformParser;
    private final HearingProcessor hearingProcessor;
    private final boolean dryRunEnabled;
    private final TelemetryService telemetryService;

    public ReplayHearingsService(
                                 CourtCaseServiceClient courtCaseServiceClient, AmazonS3 s3Client,
                                 @Value("${crime-portal-gateway-s3-bucket}") String bucketName,
                                 final MessageParser<CPHearingEvent> commonPlatformParser,
                                 final HearingProcessor hearingProcessor,
                                 @Value("${replay404.dry-run}") boolean dryRunEnabled, TelemetryService telemetryService)
    {

        this.courtCaseServiceClient = courtCaseServiceClient;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.commonPlatformParser = commonPlatformParser;
        this.hearingProcessor = hearingProcessor;
        this.dryRunEnabled = dryRunEnabled;
        this.telemetryService = telemetryService;
    }


    @NotNull
    public Supplier<String> replay(List<Hearing404> hearingsWith404) {
        return () -> {
                int numberToProcess = hearingsWith404.size();
                log.info("Total number of hearings {}", numberToProcess);
                int count = 0;
                for (Hearing404 hearing : hearingsWith404) {
                    try {
                        String id = hearing.getId();
                        String s3Path = hearing.getS3Path();
                        LocalDateTime received = hearing.getReceived().plusHours(1); // THIS IS UTC and therefore 1 hour behind the time in the S3 path

                        courtCaseServiceClient.getHearing(id).blockOptional().ifPresentOrElse(
                            (existingHearing) -> {
                                // check court-case-service and compare the last updated date with the inputted-hearing

                                // existingHearing.getLastUpdated() is using UK timezone (BST)
                                if (existingHearing.getLastUpdated().isBefore(received)) {
                                    log.info("Processing hearing {} as it has not been updated since {}", id, existingHearing.getLastUpdated());
                                    processNewOrUpdatedHearing(s3Path, id);
                                } else {
                                    log.info("Discarding hearing {} as we have a later version of it on {}", id, existingHearing.getLastUpdated());
                                    trackHearingProcessedEvent(id, "ignored");
                                }
                            },
                            () -> {
                                log.info("Processing new hearing {}", id);
                                processNewOrUpdatedHearing(s3Path, id);
                            }
                        );
                    }
                    catch (Exception e) {
                        if("hearing.prosecutionCases[0].prosecutionCaseIdentifier.caseUrn: must not be blank".equals(e.getMessage()) ||
                            "hearing.prosecutionCases: must not be empty".equals(e.getMessage())){
                            log.info("Discarding hearing {} as it is not in the correct format", hearing.getId());
                            trackHearingProcessedEvent(hearing.getId(), "ignored");
                        } else {
                            log.error("Error processing hearing with id {}", hearing.getId());
                            log.error(e.getMessage());
                            trackHearingProcessedEvent(hearing.getId(), "failed");
                        }
                    }
                    finally {
                        log.info("Processed hearing number {} of {}", ++count, numberToProcess);
                    }
                }
                log.info("Processing complete. {} of {} processed",count,numberToProcess);

            return "Finished!";
        };
    }

    private void processNewOrUpdatedHearing(String s3Path, String hearingId) {
        String message  = s3Client.getObjectAsString(bucketName, s3Path);
        CPHearingEvent cpHearingEvent;
        try {
            cpHearingEvent = commonPlatformParser.parseMessage(message, CPHearingEvent.class);
        } catch (JsonProcessingException e) {
            log.error("JsonProcessingException: failed whilst processing hearing with id {}", hearingId);
            log.error(e.getMessage());
            trackHearingProcessedEvent(hearingId, "failed");
            return;
        }
        final var hearing = cpHearingEvent.asDomain()
            .withHearingId(cpHearingEvent.getHearing().getId())
            .withHearingEventType("ConfirmedOrUpdated");

        if (dryRunEnabled) {
            log.info("Dry run - processNewOrUpdatedHearing for hearing: {}", cpHearingEvent.getHearing().getId());
            trackHearingProcessedEvent(cpHearingEvent.getHearing().getId(), "succeeded for dry run");
        } else {
            try {
                hearingProcessor.process(hearing, "pic4207-data-fix");
                log.info("Successfully processed hearing for hearing: {}", cpHearingEvent.getHearing().getId());
                trackHearingProcessedEvent(cpHearingEvent.getHearing().getId(), "succeeded");
            } catch (RuntimeException e) {
                log.error("Error processing hearing {}", cpHearingEvent.getHearing().getId());
                log.error(e.getMessage());
                trackHearingProcessedEvent(cpHearingEvent.getHearing().getId(), "failed");
            }
        }
    }

    private void trackHearingProcessedEvent(String hearingId, String status) {

        final var properties = getHearingProperties(hearingId, status);

        telemetryService.track404HearingProcessedEvent(properties);
    }

    private Map<String, String> getHearingProperties(String hearingId, String status) {
        Map<String, String> properties = new HashMap<>(10);
        properties.put("hearingId", hearingId);
        properties.put("status", status);
        properties.put("dryRun", dryRunEnabled ? "true" : "false");
        return properties;
    }
}
