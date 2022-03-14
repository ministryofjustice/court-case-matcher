package uk.gov.justice.probation.courtcasematcher.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.probation.courtcasematcher.service.SqsAdminService;

@RestController
@RequestMapping("/queue-admin")
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
