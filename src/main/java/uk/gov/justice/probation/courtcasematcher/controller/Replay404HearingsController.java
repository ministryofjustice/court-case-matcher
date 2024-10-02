package uk.gov.justice.probation.courtcasematcher.controller;


import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
public class Replay404HearingsController {

    private final String pathToCsv;

    public Replay404HearingsController(@Value("${replay404.path-to-csv}") String pathToCsv){
        this.pathToCsv = pathToCsv;
    }

    @PostMapping("/replay404Hearings")
    public String replay404Hearings() {



        // if inputted-hearing has a last updated date > court-case-service hearing
        // then call the court case matcher hearing processor

        // check how long thread timeout is
        CompletableFuture.supplyAsync(replay404s(pathToCsv));
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
    private static Supplier<String> replay404s(String path) {
        return () -> {
            try {
                System.out.println("About to read file");
                // Input
                // CSV Data from AppInsights with the hearings
                for (String hearing : Files.readAllLines(Paths.get(path), UTF_8)) {
                    String id = hearing.split(",")[0];
                    String s3Path = hearing.split(",")[1];

                    System.out.println("ID " +  id + " Path = "+ s3Path);
                    // check court-case-service and compare the last updated date with the inputted-hearing


                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // do some processing
            System.out.println("file has been read");
            return "Finished!";
        };
    }


}
