package uk.gov.justice.probation.courtcasematcher.restclient;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.event.HearingFailureEvent;
import uk.gov.justice.probation.courtcasematcher.model.domain.Address;
import uk.gov.justice.probation.courtcasematcher.model.domain.Hearing;
import uk.gov.justice.probation.courtcasematcher.model.domain.Defendant;
import uk.gov.justice.probation.courtcasematcher.model.domain.DefendantType;
import uk.gov.justice.probation.courtcasematcher.model.domain.HearingDay;
import uk.gov.justice.probation.courtcasematcher.model.domain.Name;
import uk.gov.justice.probation.courtcasematcher.model.domain.Offence;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
public class LegacyHearingRestClientIntTest {

    private static final String COURT_CODE = "B10JQ";
    private static final String CASE_NO = "12345";
    private static final String NEW_CASE_NO = "1600032981";
    static final int WEB_CLIENT_TIMEOUT_MS = 10000;

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Autowired
    private LegacyCourtCaseRestClient restClient;

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        Logger logger = (Logger) getLogger(LoggerFactory.getLogger(LegacyCourtCaseRestClient.class).getName());
        logger.addAppender(mockAppender);
    }

    @Test
    void whenGetCourtCase_thenMakeRestCallToCourtCaseService() {

        LocalDateTime startTime = LocalDateTime.of(2020, Month.JANUARY, 13, 9, 0, 0);
        Address address = Address.builder()
                .line1("27")
                .line2("Elm Place")
                .line3("Bangor")
                .postcode("ad21 5dr")
                .build();

        Offence offenceApi = Offence.builder()
                .offenceSummary("On 01/01/2016 at Town, stole Article, to the value of £100.00, belonging to Person.")
                .offenceTitle("Theft from a shop")
                .act("Contrary to section 1(1) and 7 of the Theft Act 1968.")
                .build();

        Hearing expected = Hearing.builder()
                .caseId("1246257")
                .caseNo(CASE_NO)
                .hearingDays(Collections.singletonList(HearingDay.builder()
                        .listNo("2nd")
                        .courtCode("B10JQ")
                        .courtRoom("1")
                        .sessionStartTime(startTime)
                        .build()))
                .defendants(Collections.singletonList(Defendant.builder()
                        .crn("X320741")
                        .pnc("D/1234560BC")
                        .probationStatus("CURRENT")
                        .breach(Boolean.TRUE)
                        .suspendedSentenceOrder(Boolean.FALSE)
                        .address(address)
                        .dateOfBirth(LocalDate.of(1977, Month.DECEMBER, 11))
                        .name(Name.builder().title("Mr")
                                .forename1("Dylan")
                                .forename2("Adam")
                                .surname("ARMSTRONG")
                                .build())
                        .type(DefendantType.PERSON)
                        .sex("M")
                        .offences(Collections.singletonList(offenceApi))
                        .awaitingPsr(false)
                        .preSentenceActivity(false)
                        .build()))
                .build();

        Optional<Hearing> optional = restClient.getHearing(COURT_CODE, "123456").blockOptional();

        assertThat(optional.get()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenUnknownCaseNo_whenGetCourtCase_thenReturnEmptyOptional() {

        Optional<Hearing> optional = restClient.getHearing(COURT_CODE, NEW_CASE_NO).blockOptional();

        assertThat(optional.isPresent()).isFalse();
    }

    @Builder
    public static class FailureEventMatcher implements ArgumentMatcher<HearingFailureEvent> {

        private final Class throwableClass;

        @Override
        public boolean matches(HearingFailureEvent argument) {
            return throwableClass.equals(argument.getThrowable().getClass());
        }
    }
}
