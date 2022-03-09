package uk.gov.justice.probation.courtcasematcher.controller;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.probation.courtcasematcher.service.SqsAdminService;

@RestController
public class SqsAdminController {

    private SqsAdminService sqsAdminService;

    public SqsAdminController(SqsAdminService sqsAdminService) {
        this.sqsAdminService = sqsAdminService;
    }

    @PutMapping("/retry-all-dlqs")
    public void replayDlqMessages() {
        sqsAdminService.replayDlqMessages();
    }

}
