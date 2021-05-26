package uk.gov.justice.probation.courtcasematcher.messaging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import javax.validation.ConstraintViolationException;
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
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Address;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Block;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Offence;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;

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
    @DisplayName("External Document XML")
    @Import(MessagingConfig.class)
    class ExternalDocumentMessageParser {

        @Qualifier("externalDocumentXmlParser")
        @Autowired
        public MessageParser<ExternalDocumentRequest> messageParser;

        @DisplayName("Parse a valid message with multiple sessions")
        @Test
        void whenValidExternalDocumentMessage_ThenReturnAsObject() throws IOException {
            var path = "src/test/resources/messages/xml/external-document-request-multi-session.xml";
            var content = Files.readString(Paths.get(path));

            var message = messageParser.parseMessage(content, ExternalDocumentRequest.class);

            var documents = new ArrayList<>(message.getDocumentWrapper().getDocument());

            assertThat(documents).hasSize(2);
            var document = documents.stream()
                .filter(doc -> doc.getInfo().getOuCode().equals("B01CX"))
                .findFirst().orElseThrow();

            assertThat(document.getData().getJob().getSessions()).hasSize(1);
            checkSession(document.getData().getJob().getSessions().get(0));

            // Check fallback for ou_code when it is not in the session
            var document2 = documents.stream()
                .filter(doc -> doc.getInfo().getOuCode().equals("B01CY"))
                .findFirst().orElseThrow();
            var session = document2.getData().getJob().getSessions().get(0);
            assertThat(session.getCourtCode()).isEqualTo("B01CY");
        }

        @DisplayName("Invalid source info name for date of hearing / ou code and missing case no")
        @Test
        void whenInvalidMessage() throws IOException {
            var path = "src/test/resources/messages/xml/external-document-request-invalid.xml";
            var content = Files.readString(Paths.get(path));

            var thrown = catchThrowable(() -> messageParser.parseMessage(content, ExternalDocumentRequest.class));

            var ex = (ConstraintViolationException) thrown;
            assertThat(ex.getConstraintViolations()).hasSize(3);
            var docInfoPath = "documentWrapper.document[0].info";
            var firstSessionPath = "documentWrapper.document[0].data.job.sessions[0]";
            assertThat(ex.getConstraintViolations()).anyMatch(cv -> cv.getMessage().equals("must not be null")
                && cv.getPropertyPath().toString().equals(docInfoPath + ".dateOfHearing"));
            assertThat(ex.getConstraintViolations()).anyMatch(cv -> cv.getMessage().equals("must not be null")
                && cv.getPropertyPath().toString().equals(docInfoPath + ".ouCode"));
            assertThat(ex.getConstraintViolations()).anyMatch(cv -> cv.getMessage().equals("must not be blank")
                && cv.getPropertyPath().toString().equals(firstSessionPath + ".blocks[0].cases[0].caseNo"));
        }
    }

    @Nested
    @DisplayName("Case JSON")
    @Import(MessagingConfig.class)
    class CaseJsonMessageParser {

        @Qualifier("caseJsonParser")
        @Autowired
        public MessageParser<Case> messageParser;

        @DisplayName("Parse a valid message")
        @Test
        void whenValidCase_ThenReturn() throws IOException {
            var path = "src/test/resources/messages/json/case.json";
            var content = Files.readString(Paths.get(path));

            var aCase = messageParser.parseMessage(content, Case.class);
            checkCase(aCase);
        }

        @DisplayName("Parse an invalid message")
        @Test
        void whenInvalidCase_ThenThrowConstraintViolation() throws IOException {
            var path = "src/test/resources/messages/json/case-invalid.json";
            var content = Files.readString(Paths.get(path));

            var thrown = catchThrowable(() -> messageParser.parseMessage(content, Case.class));

            var ex = (ConstraintViolationException) thrown;
            assertThat(ex.getConstraintViolations()).hasSize(1);
            assertThat(ex.getConstraintViolations()).anyMatch(cv -> cv.getMessage().equals("must not be blank")
                && cv.getPropertyPath().toString().equals("caseNo"));
        }
    }

    private static void checkSession(Session session) {
        assertThat(session.getId()).isEqualTo(556805L);
        assertThat(session.getDateOfHearing()).isEqualTo(HEARING_DATE);
        assertThat(session.getCourtCode()).isEqualTo("B01CX");
        assertThat(session.getCourtName()).isEqualTo("Camberwell Green");
        assertThat(session.getCourtRoom()).isEqualTo("00");
        assertThat(session.getStart()).isEqualTo(START_TIME);
        assertThat(session.getSessionStartTime()).isEqualTo(SESSION_START_TIME);
        assertThat(session.getEnd()).isEqualTo(LocalTime.of(13, 5));
        assertThat(session.getBlocks()).hasSize(1);
        checkBlock(session.getBlocks().get(0));
    }

    private static void checkBlock(Block block) {
        assertThat(block.getCases()).hasSize(2);
        checkCase(block.getCases().stream().filter(aCase -> aCase.getCaseNo().equals("1600032953")).findFirst().orElseThrow());
    }

    private static void checkCase(Case aCase) {
        // Fields populated from the session
        assertThat(aCase.getDefendantAge()).isEqualTo("20");
        assertThat(aCase.getCaseId()).isEqualTo(1217464);
        assertThat(aCase.getDefendantName()).isEqualTo("Mr Arthur MORGAN");
        assertThat(aCase.getName()).isEqualTo(Name.builder()
                                                .title("Mr")
                                                .forename1("Arthur")
                                                .surname("MORGAN").build());
        assertThat(aCase.getDefendantType()).isEqualTo("P");
        assertThat(aCase.getDefendantSex()).isEqualTo("N");
        assertThat(aCase.getDefendantAge()).isEqualTo("20");
        assertThat(aCase.getPnc()).isEqualTo("2004/0012345U");
        assertThat(aCase.getCro()).isEqualTo("11111/79J");
        assertThat(aCase.getDefendantAddress()).usingRecursiveComparison().isEqualTo(Address.builder()
                                                                    .line1("39 The Street")
                                                                    .line2("Newtown")
                                                                    .pcode("NT4 6YH").build());
        assertThat(aCase.getDefendantDob()).isEqualTo(LocalDate.of(1975, Month.JANUARY, 1));
        assertThat(aCase.getNationality1()).isEqualTo("Angolan");
        assertThat(aCase.getNationality2()).isEqualTo("Austrian");
        assertThat(aCase.getSeq()).isEqualTo(1);
        assertThat(aCase.getListNo()).isEqualTo("1st");
        assertThat(aCase.getOffences()).hasSize(1);
        checkOffence(aCase.getOffences().get(0));
    }

    private static void checkOffence(Offence offence) {
        assertThat(offence.getSeq()).isEqualTo(1);
        assertThat(offence.getTitle()).isEqualTo("Theft from a shop");
        assertThat(offence.getSummary()).isEqualTo("On 01/01/2016 at Town, stole Article, to the value of £100.00, belonging to Person.");
        assertThat(offence.getAct()).isEqualTo("Contrary to section 1(1) and 7 of the Theft Act 1968.");
    }

}
