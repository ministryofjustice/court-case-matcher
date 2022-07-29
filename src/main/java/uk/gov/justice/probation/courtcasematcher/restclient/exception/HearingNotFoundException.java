package uk.gov.justice.probation.courtcasematcher.restclient.exception;

public class HearingNotFoundException extends Exception {
    public HearingNotFoundException(String courtCode, String caseNo) {
        super(String.format("Case no '%s' not found for court code '%s", caseNo, courtCode));
    }

    public HearingNotFoundException(String caseId) {
        super(String.format("Case Id '%s' not found", caseId));
    }
}
