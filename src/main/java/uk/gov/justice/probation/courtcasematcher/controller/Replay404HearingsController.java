package uk.gov.justice.probation.courtcasematcher.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.justice.probation.courtcasematcher.service.ReplayHearingsService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class Replay404HearingsController {

    private final ReplayHearingsService replayHearingsService;

    Replay404HearingsController(ReplayHearingsService replayHearingsService){
        this.replayHearingsService = replayHearingsService;
    }

    @PostMapping(path="/replay404Hearings", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String replay404Hearings(@RequestBody MultipartFile file) throws IOException {

        InputStream inputStream = file.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        List<Hearing404> hearing404s = br.lines().map(hearing -> {
            String[] hearingDetails = hearing.split(",");
            String id = hearingDetails[0];
            String s3Path = hearingDetails[1];
            LocalDateTime received = LocalDateTime.from(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").parse(hearingDetails[2])); // THIS IS UTC and therefore 1 hour behind the time in the S3 path
            return new Hearing404(id, s3Path, received);
        }).collect(Collectors.toList());

        log.info("Starting to replay 404 hearings");
        CompletableFuture.supplyAsync(replayHearingsService.replay(hearing404s));

        return "OK";
    }
}
