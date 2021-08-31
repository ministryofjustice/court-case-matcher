package uk.gov.justice.probation.courtcasematcher.restclient.model;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcasematcher.model.domain.Address;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;
import uk.gov.justice.probation.courtcasematcher.restclient.model.courtcaseservice.CCSCourtCase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType.PERSON;

class CCSCourtCaseTest {

    @Test
    public void map() {
        final CourtCase courtCase = buildCase();

        final var courtCaseRequest = CCSCourtCase.of(courtCase);

        assertThat(courtCaseRequest).usingRecursiveComparison()
                .ignoringFields("probationStatusActual")
                .isEqualTo(courtCase);
    }

    @Test
    public void mapBack() {
        final CourtCase original = buildCase();

        final var actual = CCSCourtCase.of(original).asDomain();

        assertThat(actual).isEqualTo(original);
    }

    private CourtCase buildCase() {
        return CourtCase.builder()
                .caseId("1234")
                .caseNo("5678")
                .courtRoom("ROOM 1")
                .courtCode("B10JQ")
                .awaitingPsr(true)
                .breach(true)
                .crn("crn")
                .cro("cro")
                .pnc("pnc")
                .defendantDob(LocalDate.of(2000, 1, 1))
                .defendantSex("FEMALE")
                .defendantType(PERSON)
                .listNo("1")
                .nationality1("nat1")
                .nationality2("nat2")
                .isNew(true)
                .preSentenceActivity(true)
                .previouslyKnownTerminationDate(LocalDate.of(2001, 1, 1))
                .probationStatus("Current")
                .sessionStartTime(LocalDateTime.of(2002, 1, 1, 1, 1))
                .suspendedSentenceOrder(true)
                .defendantName("Frederiche")
                .name(Name.builder()
                        .title("Ms")
                        .forename1("Freddy")
                        .forename2("Freddo")
                        .forename3("Freddeline")
                        .surname("McFred")
                        .build())
                .offences(Collections.singletonList(Offence.builder()
                        .act("act")
                        .offenceSummary("summary")
                        .offenceTitle("title")
                        .sequenceNumber(1)
                        .build()))
                .defendantAddress(Address.builder()
                        .line1("line1")
                        .line2("line2")
                        .line3("line3")
                        .line4("line4")
                        .line5("line5")
                        .build())
                .build();
    }

}
