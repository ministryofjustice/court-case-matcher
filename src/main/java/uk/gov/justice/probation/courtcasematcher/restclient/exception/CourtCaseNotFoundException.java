package uk.gov.justice.probation.courtcasematcher.restclient.exception;

public class CourtCaseNotFoundException extends Exception {
    public CourtCaseNotFoundException(String courtCode, String caseNo) {
        super(String.format("Case no '%s' not found for court code '%s", caseNo, courtCode));
    }

    public CourtCaseNotFoundException(String caseId) {
        super(String.format("Case Id '%s' not found", caseId));
    }
}
