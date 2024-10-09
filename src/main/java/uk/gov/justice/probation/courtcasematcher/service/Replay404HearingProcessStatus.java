package uk.gov.justice.probation.courtcasematcher.service;

public enum Replay404HearingProcessStatus {
    FAILED("FAILED"),
    OUTDATED("OUTDATED"),
    SUCCEEDED("SUCCEEDED"),
    INVALID("INVALID");

    public final String status;

    Replay404HearingProcessStatus(String status) {
        this.status = status;
    }
}
