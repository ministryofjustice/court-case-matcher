package uk.gov.justice.probation.courtcasematcher.service;

import software.amazon.awssdk.services.s3.S3Client;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import uk.gov.justice.probation.courtcasematcher.controller.Hearing404;
import uk.gov.justice.probation.courtcasematcher.messaging.HearingProcessor;
import uk.gov.justice.probation.courtcasematcher.messaging.MessageParser;
import uk.gov.justice.probation.courtcasematcher.messaging.model.commonplatform.CPHearingEvent;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseServiceClient;

import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Service
public class ReplayHearingsService {


    private final CourtCaseServiceClient courtCaseServiceClient;
    private final S3Client s3Client;
    private final String bucketName;
    private final MessageParser<CPHearingEvent> commonPlatformParser;
    private final HearingProcessor hearingProcessor;
    private final boolean dryRunEnabled;
    private final TelemetryService telemetryService;

    public ReplayHearingsService(
        CourtCaseServiceClient courtCaseServiceClient,
        final S3Client s3Client,
        @Value("${crime-portal-gateway-s3-bucket}") String bucketName,
        final MessageParser<CPHearingEvent> commonPlatformParser,
        final HearingProcessor hearingProcessor,
        @Value("${replay404.dry-run}") boolean dryRunEnabled, TelemetryService telemetryService) {

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
                                processNewOrUpdatedHearing(s3Path);
                            } else {
                                log.info("Discarding hearing {} as we have a later version of it on {}", id, existingHearing.getLastUpdated());
                                trackHearingProcessedEvent(id, Replay404HearingProcessStatus.OUTDATED, Collections.emptyMap());
                            }
                        },
                        () -> {
                            log.info("Processing new hearing {}", id);
                            processNewOrUpdatedHearing(s3Path);
                        }
                    );
                } catch (ConstraintViolationException e) {
                    log.info("Discarding hearing {} as it is not in the correct format", hearing.getId());
                    trackHearingProcessedEvent(hearing.getId(), Replay404HearingProcessStatus.INVALID, Map.of("reason", e.getMessage()));
                } catch (Exception e) {
                    log.error("Error processing hearing with id {}", hearing.getId());
                    try {
                        log.error(e.getMessage());
                        trackHearingProcessedEvent(hearing.getId(), Replay404HearingProcessStatus.FAILED,  Map.of("reason", e.getMessage()));
                    } catch(Exception ex) {
                        log.error(Level.ERROR.name(), "Unknown", ex);
                        trackHearingProcessedEvent(hearing.getId(), Replay404HearingProcessStatus.FAILED,  Map.of("reason", "Unknown"));
                    }


                } finally {
                    log.info("Processed hearing number {} of {}", ++count, numberToProcess);
                }
            }
            log.info("Processing complete. {} of {} processed", count, numberToProcess);

            return "Finished!";
        };
    }

    private void processNewOrUpdatedHearing(String s3Path) {
        String message = s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucketName).key(s3Path).build()).asString(StandardCharsets.UTF_8);
        CPHearingEvent cpHearingEvent;
        try {
            cpHearingEvent = commonPlatformParser.parseMessage(message, CPHearingEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        final var hearings = cpHearingEvent.asDomain(cprExtractor)
            .stream()
            .map(h -> h.withHearingId(cpHearingEvent.getHearing().getId())
                .withHearingEventType("ConfirmedOrUpdated"))
            .toList();

        if (dryRunEnabled) {
            log.info("Dry run - processNewOrUpdatedHearing for hearing: {}", cpHearingEvent.getHearing().getId());
            trackHearingProcessedEvent(cpHearingEvent.getHearing().getId(), Replay404HearingProcessStatus.SUCCEEDED, Collections.emptyMap());
        } else {
            hearings.forEach(hearing -> {
                hearingProcessor.process(hearing, "pic4207-data-fix");
                log.info("Successfully processed hearing for hearing: {}", cpHearingEvent.getHearing().getId());
                trackHearingProcessedEvent(cpHearingEvent.getHearing().getId(), Replay404HearingProcessStatus.SUCCEEDED, Collections.emptyMap());
            });
        }
    }

    private void trackHearingProcessedEvent(String hearingId, Replay404HearingProcessStatus status, Map<String, String> additionalProperties) {

        final var properties = getHearingProperties(hearingId, status.status, additionalProperties);

        telemetryService.track404HearingProcessedEvent(properties);
    }

    private Map<String, String> getHearingProperties(String hearingId, String status, Map<String, String> additionalProperties) {
        Map<String, String> properties = new HashMap<>(10);
        properties.put("hearingId", hearingId);
        properties.put("status", status);
        properties.put("dryRun", dryRunEnabled ? "true" : "false");
        properties.putAll(additionalProperties);
        return properties;
    }
}
