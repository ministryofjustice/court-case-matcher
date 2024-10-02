package uk.gov.justice.probation.courtcasematcher.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Replay404HearingsController {


    @PostMapping("/replay404Hearings")
    public String replay404Hearings() {

        // Input
        // CSV Data from AppInsights with the hearings

        // check court-case-service and compare the last updated date with the inputted-hearing
        // if inputted-hearing has a last updated date > court-case-service hearing
        // then call the court case matcher hearing processor


        // processing will take a long time, so we need to prevent timeouts during long processing
        // should provide logs to show progression

        // Output
        // HTTP status

        // How to test end-2-end
        // put a few updated hearings (minor updates) in dev/preprod to the S3 bucket
        // call this endpoint to pick up on those unprocessed hearings in the s3 bucket and process them


        return "OK";
    }

}
