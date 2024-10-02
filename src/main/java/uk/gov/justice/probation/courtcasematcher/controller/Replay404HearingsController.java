package uk.gov.justice.probation.courtcasematcher.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Replay404HearingsController {


    @PostMapping("/replay404Hearings")
    public String replay404Hearings() {
        return "OK";
    }

}
