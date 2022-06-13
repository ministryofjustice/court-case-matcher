package uk.gov.justice.probation.courtcasematcher.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.model.domain.Address;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;
import uk.gov.justice.probation.courtcasematcher.model.domain.PhoneNumber;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType.ORGANISATION;
import static uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType.PERSON;

@ExtendWith(MockitoExtension.class)
class IncomingCourtCaseComparatorTest {

    @DisplayName("Received case is same as existing case")
    @Test
    void givenReceivedCourtCaseIsSameAsExistingCase_ThenReturnFalse() {
        var courtCaseReceived = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("SHF")
                        .build()))
                .defendants(Collections.singletonList(Defendant.builder()
                        .cro("CRO")
                        .type(PERSON)
                        .build()))
                .build();

        var existingCourtCase = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("SHF")
                        .build()))
                .defendants(Collections.singletonList(Defendant.builder()
                        .cro("CRO")
                        .type(PERSON)
                        .build()))
                .build();

        assertFalse(IncomingCourtCaseComparator.hasCourtCaseChanged(courtCaseReceived, existingCourtCase));
    }

    @DisplayName("Received case contains more defendant than the existing case")
    @Test
    void givenReceivedCourtCaseContainsMoreDefendant_ThenReturnTrue() {
        var defendant1 = Defendant.builder()
                .cro("CRO")
                .type(PERSON)
                .build();
        var defendant2 = Defendant.builder()
                .cro("CRO-another")
                .type(PERSON)
                .build();
        var courtCaseReceived = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("SHF")
                        .build()))
                .defendants(Arrays.asList(defendant1, defendant2))
                .build();

        var existingCourtCase = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("SHF")
                        .build()))
                .defendants(Collections.singletonList(defendant1))
                .build();

        assertTrue(IncomingCourtCaseComparator.hasCourtCaseChanged(courtCaseReceived, existingCourtCase));
    }

    @DisplayName("Received case has different address")
    @Test
    void givenReceivedCourtCaseHasDifferentAddress_ThenReturnTrue() {
        var courtCaseReceived = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("SHF")
                        .build()))
                .defendants(Collections.singletonList(Defendant.builder()
                        .cro("CRO")
                        .type(PERSON)
                                .address(Address.builder()
                                        .postcode("Cf23 4as")
                                        .build())
                        .build()))
                .build();

        var existingCourtCase = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("SHF")
                        .build()))
                .defendants(Collections.singletonList(Defendant.builder()
                        .cro("CRO")
                        .type(PERSON)
                        .address(Address.builder()
                                .postcode("Cf10 4as")
                                .build())
                        .build()))
                .build();

        assertTrue(IncomingCourtCaseComparator.hasCourtCaseChanged(courtCaseReceived, existingCourtCase));
    }

    @DisplayName("Received case has hearing day details")
    @Test
    void givenReceivedCourtCaseContainsHearingDays_ThenReturnTrue() {
        var courtCaseReceived = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("SHF")
                        .build()))
                .defendants(Collections.singletonList(Defendant.builder()
                        .cro("CRO")
                        .type(PERSON)
                        .address(Address.builder()
                                .postcode("Cf23 4as")
                                .build())
                        .build()))
                .build();

        var existingCourtCase = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .cro("CRO")
                        .type(PERSON)
                        .address(Address.builder()
                                .postcode("Cf23 4as")
                                .build())
                        .build()))
                .build();

        assertTrue(IncomingCourtCaseComparator.hasCourtCaseChanged(courtCaseReceived, existingCourtCase));
    }

    @DisplayName("Received case has different court details")
    @Test
    void givenReceivedCourtCaseHasDifferentCourt_ThenReturnTrue() {
        var courtCaseReceived = CourtCase.builder()
                .urn("URN-OTHER")
                .defendants(Collections.singletonList(Defendant.builder()
                        .cro("CRO")
                        .type(PERSON)
                        .address(Address.builder()
                                .postcode("Cf23 4as")
                                .build())
                        .build()))
                .build();

        var existingCourtCase = CourtCase.builder()
                .urn("URN")
                .defendants(Collections.singletonList(Defendant.builder()
                        .cro("CRO")
                        .type(PERSON)
                        .address(Address.builder()
                                .postcode("Cf23 4as")
                                .build())
                        .build()))
                .build();

        assertTrue(IncomingCourtCaseComparator.hasCourtCaseChanged(courtCaseReceived, existingCourtCase));
    }

    @DisplayName("Received case has a defendant with different pnc")
    @Test
    void givenReceivedCourtCaseContainsDefendantWithDifferentPnc_ThenReturnTrue() {
        var courtCaseReceived = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .cro("CRO")
                        .pnc("PNC1")
                        .type(PERSON)
                        .address(Address.builder()
                                .postcode("Cf23 4as")
                                .build())
                        .build()))
                .build();

        var existingCourtCase = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .cro("CRO")
                        .pnc("PNC")
                        .type(PERSON)
                        .address(Address.builder()
                                .postcode("Cf23 4as")
                                .build())
                        .build()))
                .build();

        assertTrue(IncomingCourtCaseComparator.hasCourtCaseChanged(courtCaseReceived, existingCourtCase));
    }

    @DisplayName("Received case has defendant name changed")
    @Test
    void givenReceivedCourtCaseContainsDefendantWithDifferentName_ThenReturnTrue() {
        var courtCaseReceived = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .name(Name.builder()
                                .forename1("forename1")
                                .surname("surnameOther")
                                .build())
                        .cro("CRO")
                        .type(PERSON)
                        .address(Address.builder()
                                .postcode("Cf23 4as")
                                .build())
                        .build()))
                .build();

        var existingCourtCase = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .name(Name.builder()
                                .forename1("forename1")
                                .surname("surname1")
                                .build())
                        .cro("CRO")
                        .type(PERSON)
                        .address(Address.builder()
                                .postcode("Cf23 4as")
                                .build())
                        .build()))
                .build();

        assertTrue(IncomingCourtCaseComparator.hasCourtCaseChanged(courtCaseReceived, existingCourtCase));
    }

    @DisplayName("Received case has a defendant with different phone number")
    @Test
    void givenReceivedCourtCaseContainsDefendantWithDifferentPhoneNumber_ThenReturnTrue() {
        var courtCaseReceived = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .phoneNumber(PhoneNumber.builder()
                                .mobile("07564328988")
                                .build())
                        .cro("CRO")
                        .type(PERSON)
                        .address(Address.builder()
                                .postcode("Cf23 4as")
                                .build())
                        .build()))
                .build();

        var existingCourtCase = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .phoneNumber(PhoneNumber.builder()
                                .mobile("07564328999")
                                .build())
                        .cro("CRO")
                        .type(PERSON)
                        .address(Address.builder()
                                .postcode("Cf23 4as")
                                .build())
                        .build()))
                .build();

        assertTrue(IncomingCourtCaseComparator.hasCourtCaseChanged(courtCaseReceived, existingCourtCase));
    }

    @DisplayName("Received case has a defendant with different DOB")
    @Test
    void givenReceivedCourtCaseContainsDefendantWithDifferentDob_ThenReturnTrue() {
        var courtCaseReceived = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .cro("CRO")
                        .type(PERSON)
                        .dateOfBirth(LocalDate.of(1984, 1, 1))
                        .address(Address.builder()
                                .postcode("Cf23 4as")
                                .build())
                        .build()))
                .build();

        var existingCourtCase = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .cro("CRO")
                        .type(PERSON)
                        .dateOfBirth(LocalDate.of(1985, 1, 1))
                        .address(Address.builder()
                                .postcode("Cf23 4as")
                                .build())
                        .build()))
                .build();

        assertTrue(IncomingCourtCaseComparator.hasCourtCaseChanged(courtCaseReceived, existingCourtCase));
    }

    @DisplayName("Received case has a defendant with different type")
    @Test
    void givenReceivedCourtCaseContainsDefendantWithDifferentType_ThenReturnTrue() {
        var courtCaseReceived = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .cro("CRO")
                        .type(PERSON)
                        .address(Address.builder()
                                .postcode("Cf23 4as")
                                .build())
                        .build()))
                .build();

        var existingCourtCase = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .cro("CRO")
                        .type(ORGANISATION)
                        .address(Address.builder()
                                .postcode("Cf23 4as")
                                .build())
                        .build()))
                .build();

        assertTrue(IncomingCourtCaseComparator.hasCourtCaseChanged(courtCaseReceived, existingCourtCase));
    }

    @DisplayName("Received case has a defendant with different sex")
    @Test
    void givenReceivedCourtCaseContainsDefendantWithDifferentSex_ThenReturnTrue() {
        var courtCaseReceived = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .cro("CRO")
                        .type(PERSON)
                        .sex("MALE")
                        .address(Address.builder()
                                .postcode("Cf23 4as")
                                .build())
                        .build()))
                .build();

        var existingCourtCase = CourtCase.builder()
                .defendants(Collections.singletonList(Defendant.builder()
                        .cro("CRO")
                        .type(PERSON)
                        .sex("FEMALE")
                        .address(Address.builder()
                                .postcode("Cf23 4as")
                                .build())
                        .build()))
                .build();

        assertTrue(IncomingCourtCaseComparator.hasCourtCaseChanged(courtCaseReceived, existingCourtCase));
    }

    @DisplayName("Received case contains defendant with different CRO")
    @Test
    void givenReceivedCourtCaseContainsDefendantWithDifferentCRO_ThenReturnTrue() {
        var defendant1 = Defendant.builder()
                .cro("CRO")
                .type(PERSON)
                .build();
        var defendantWithDifferentCRO = Defendant.builder()
                .cro("CRO-another")
                .type(PERSON)
                .build();
        var courtCaseReceived = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("SHF")
                        .build()))
                .defendants(Collections.singletonList(defendantWithDifferentCRO))
                .build();

        var existingCourtCase = CourtCase.builder()
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .courtCode("SHF")
                        .build()))
                .defendants(Collections.singletonList(defendant1))
                .build();

        assertTrue(IncomingCourtCaseComparator.hasCourtCaseChanged(courtCaseReceived, existingCourtCase));
    }
}