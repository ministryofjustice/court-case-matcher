package uk.gov.justice.probation.courtcasematcher.controller;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@AllArgsConstructor
@Data
public class Hearing404 {
    String id;
    String s3Path;
    LocalDateTime received;

}
