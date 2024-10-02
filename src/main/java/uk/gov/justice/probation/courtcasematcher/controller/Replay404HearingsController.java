package uk.gov.justice.probation.courtcasematcher.controller;


import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseServiceClient;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
public class Replay404HearingsController {

    private final String pathToCsv;
    private final CourtCaseServiceClient courtCaseServiceClient;

    public Replay404HearingsController(@Value("${replay404.path-to-csv}") String pathToCsv, CourtCaseServiceClient courtCaseServiceClient){
        this.pathToCsv = pathToCsv;
        this.courtCaseServiceClient = courtCaseServiceClient;
    }

    @PostMapping("/replay404Hearings")
    public String replay404Hearings() {
        // put some proper exception handling in place


        // if inputted-hearing has a last updated date > court-case-service hearing
        // then call the court case matcher hearing processor

        // check how long thread timeout is
        CompletableFuture.supplyAsync(replay404s(pathToCsv, courtCaseServiceClient));
        // should provide logs to show progression

        // Output
        // HTTP status

        // How to test end-2-end
        // put a few updated hearings (minor updates) in dev/preprod to the S3 bucket
        // call this endpoint to pick up on those unprocessed hearings in the s3 bucket and process them

        // dry run option?
        System.out.println("Finished processing");

        return "OK";
    }

    @NotNull
    private static Supplier<String> replay404s(String path, CourtCaseServiceClient courtCaseServiceClient) {
        return () -> {
            try {
                System.out.println("About to read file");
                // Input
                // CSV Data from AppInsights with the hearings
                for (String hearing : Files.readAllLines(Paths.get(path), UTF_8)) {
                    String[] hearingDetails = hearing.split(",");
                    String id = hearingDetails[0];
                    String s3Path = hearingDetails[1];
                    LocalDateTime received = LocalDateTime.from(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").parse(hearingDetails[2])); // THIS IS UTC and therefore 1 hour behind the time in the S3 path

                    System.out.println("ID " +  id + " Path = "+ s3Path);

                    courtCaseServiceClient.getHearing(id).blockOptional().ifPresentOrElse(
                        (existingHearing) -> {
                            // check court-case-service and compare the last updated date with the inputted-hearing
                            if (existingHearing.getLastUpdated().isBefore(received)) {
                                System.out.println("Processing hearing " + id + " as it has not been updated since " + existingHearing.getLastUpdated());
                                processNewOrUpdatedHearing();
                            }else {
                                System.out.println("Discarding hearing " + id + " as we have a later version of it on " + existingHearing.getLastUpdated());
                            }
                        },
                        () -> {
                            System.out.println("Processing hearing " + id + " as it is new");
                            processNewOrUpdatedHearing();
                        }
                    );


                    //dry run mode here


                }

            } catch (Exception e) {
                System.out.println(e.getMessage());
                throw new RuntimeException(e);
            }
            System.out.println("file has been read");
            return "Finished!";
        };
    }

    private static void processNewOrUpdatedHearing() {
        System.out.println("Processing hearing ");
    }


}
