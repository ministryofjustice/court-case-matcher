package uk.gov.justice.probation.courtcasematcher.restclient.exception;

public class CprCanonicalRecordNotFoundException extends Exception {
    public CprCanonicalRecordNotFoundException(String cprUUID) {
        super(String.format("Cpr canonical record not found for '%s'", cprUUID));
    }
}
