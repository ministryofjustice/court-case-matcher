package uk.gov.justice.probation.courtcasematcher.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.justice.probation.courtcasematcher.application.MessagingConfig;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraAddress;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraCase;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraName;
import uk.gov.justice.probation.courtcasematcher.messaging.model.libra.LibraOffence;

import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ExtendWith(SpringExtension.class)
@DisplayName("Message Parser Test")
@Profile("test")
class MessageParserTest {

    private static final LocalDate HEARING_DATE = LocalDate.of(2020, Month.FEBRUARY, 20);
    private static final LocalTime START_TIME = LocalTime.of(9, 1);
    private static final LocalDateTime SESSION_START_TIME = LocalDateTime.of(HEARING_DATE, START_TIME);

    @Nested
    @DisplayName("Case JSON")
    @Import(MessagingConfig.class)
    class CaseJsonMessageParser {

        @Qualifier("caseJsonParser")
        @Autowired
        public MessageParser<LibraCase> messageParser;

        @DisplayName("Parse a valid message")
        @Test
        void whenValidCase_ThenReturn() throws IOException {
            var path = "src/test/resources/messages/json/case.json";
            var content = Files.readString(Paths.get(path));

            var aCase = messageParser.parseMessage(content, LibraCase.class);
            checkCase(aCase);
        }

        @DisplayName("Parse an invalid message")
        @Test
        void whenInvalidCase_ThenThrowConstraintViolation() throws IOException {
            var path = "src/test/resources/messages/json/case-invalid.json";
            var content = Files.readString(Paths.get(path));

            var thrown = catchThrowable(() -> messageParser.parseMessage(content, LibraCase.class));

            var ex = (ConstraintViolationException) thrown;
            assertThat(ex.getConstraintViolations()).hasSize(1);
            assertThat(ex.getConstraintViolations()).anyMatch(cv -> cv.getMessage().equals("must not be blank")
                && cv.getPropertyPath().toString().equals("caseNo"));
        }
    }

    private static void checkCase(LibraCase aLibraCase) {
        // Fields populated from the session
        assertThat(aLibraCase.getDefendantAge()).isEqualTo("20");
        assertThat(aLibraCase.getCaseId()).isEqualTo(1217464);
        assertThat(aLibraCase.getDefendantName()).isEqualTo("Mr Arthur MORGAN");
        assertThat(aLibraCase.getName()).isEqualTo(LibraName.builder()
                                                .title("Mr")
                                                .forename1("Arthur")
                                                .surname("MORGAN").build());
        assertThat(aLibraCase.getDefendantType()).isEqualTo("P");
        assertThat(aLibraCase.getDefendantSex()).isEqualTo("N");
        assertThat(aLibraCase.getDefendantAge()).isEqualTo("20");
        assertThat(aLibraCase.getPnc()).isEqualTo("2004/0012345U");
        assertThat(aLibraCase.getCro()).isEqualTo("11111/79J");
        assertThat(aLibraCase.getDefendantAddress()).usingRecursiveComparison().isEqualTo(LibraAddress.builder()
                                                                    .line1("39 The Street")
                                                                    .line2("Newtown")
                                                                    .pcode("NT4 6YH").build());
        assertThat(aLibraCase.getDefendantDob()).isEqualTo(LocalDate.of(1975, Month.JANUARY, 1));
        assertThat(aLibraCase.getNationality1()).isEqualTo("Angolan");
        assertThat(aLibraCase.getNationality2()).isEqualTo("Austrian");
        assertThat(aLibraCase.getSeq()).isEqualTo(1);
        assertThat(aLibraCase.getListNo()).isEqualTo("1st");
        assertThat(aLibraCase.getOffences()).hasSize(1);
        checkOffence(aLibraCase.getOffences().get(0));
    }

    private static void checkOffence(LibraOffence offence) {
        assertThat(offence.getSeq()).isEqualTo(1);
        assertThat(offence.getTitle()).isEqualTo("Theft from a shop");
        assertThat(offence.getSummary()).isEqualTo("On 01/01/2016 at Town, stole Article, to the value of £100.00, belonging to Person.");
        assertThat(offence.getAct()).isEqualTo("Contrary to section 1(1) and 7 of the Theft Act 1968.");
    }

}
