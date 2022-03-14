package uk.gov.justice.probation.courtcasematcher.restclient;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.MatchRequest;
import uk.gov.justice.probation.courtcasematcher.restclient.model.offendersearch.OffenderSearchMatchType;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
public class OffenderSearchRestClientIntTest {

    @Autowired
    private OffenderSearchRestClient restClient;

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @Nested
    class Match {

        private final MatchRequest.Factory matchRequestFactory = new MatchRequest.Factory();

        @Test
        public void givenSingleMatchReturned_whenMatch_thenReturnIt() {
            var name = uk.gov.justice.probation.courtcasematcher.model.domain.Name.builder().forename1("Arthur").surname("MORGAN").build();
            var match = restClient.match(matchRequestFactory.buildFrom(null, name, LocalDate.of(1975, 1, 1))).blockOptional();

            assertThat(match).isPresent();
            assertThat(match.get().getMatchedBy()).isEqualTo(OffenderSearchMatchType.ALL_SUPPLIED);
            assertThat(match.get().getMatches().size()).isEqualTo(1);
            assertThat(match.get().isExactMatch()).isTrue();

            var offender = match.get().getMatches().get(0).getOffender();
            assertThat(offender.getOtherIds().getCrn()).isEqualTo("X346204");
            assertThat(offender.getOtherIds().getCroNumber()).isEqualTo("1234ABC");
            assertThat(offender.getOtherIds().getPncNumber()).isEqualTo("ABCD1234");
            assertThat(offender.getProbationStatus().getStatus()).isEqualTo("CURRENT");
            assertThat(offender.getProbationStatus().getInBreach()).isTrue();
            assertThat(offender.getProbationStatus().isPreSentenceActivity()).isFalse();
            assertThat(offender.getProbationStatus().isAwaitingPsr()).isTrue();
            assertThat(offender.getProbationStatus().getPreviouslyKnownTerminationDate()).isEqualTo(LocalDate.of(2020, Month.FEBRUARY, 2));
        }

        @Test
        public void givenSingleMatchReturned_whenMatchWithPncNoDob_thenReturnIt() {
            var name = uk.gov.justice.probation.courtcasematcher.model.domain.Name.builder().forename1("Arthur").surname("MORGAN").build();
            var match = restClient.match(matchRequestFactory.buildFrom("2004/0012345U", name, LocalDate.of(1975, 1, 1))).blockOptional();

            assertThat(match).isPresent();
            assertThat(match.get().getMatchedBy()).isEqualTo(OffenderSearchMatchType.ALL_SUPPLIED);
            assertThat(match.get().getMatches().size()).isEqualTo(1);
            assertThat(match.get().isExactMatch()).isTrue();

            var offender = match.get().getMatches().get(0).getOffender();
            assertThat(offender.getOtherIds().getCrn()).isEqualTo("X346204");
            assertThat(offender.getOtherIds().getCroNumber()).isEqualTo("1234ABC");
            assertThat(offender.getOtherIds().getPncNumber()).isEqualTo("2004/0012345U");
        }

        @Test
        public void givenSingleMatchReturned_whenMatchWithPncWithDob_thenReturnIt() {
            var name = uk.gov.justice.probation.courtcasematcher.model.domain.Name.builder().forename1("Arthur").surname("MORGAN").build();
            matchRequestFactory.setUseDobWithPnc(true);
            var match = restClient.match(matchRequestFactory.buildFrom("2004/0012345U", name, LocalDate.of(1975, 1, 1))).blockOptional();

            assertThat(match).isPresent();
            assertThat(match.get().getMatchedBy()).isEqualTo(OffenderSearchMatchType.ALL_SUPPLIED);
            assertThat(match.get().getMatches().size()).isEqualTo(1);
            assertThat(match.get().isExactMatch()).isTrue();

            var offender = match.get().getMatches().get(0).getOffender();
            assertThat(offender.getOtherIds().getCrn()).isEqualTo("X346204");
            assertThat(offender.getOtherIds().getCroNumber()).isEqualTo("1234ABC");
            assertThat(offender.getOtherIds().getPncNumber()).isEqualTo("2004/0012345U");
        }

        @Test
        public void givenSingleMatchReturned_whenMatch_thenVerifyMono() {
            var name = uk.gov.justice.probation.courtcasematcher.model.domain.Name.builder().forename1("Arthur").surname("MORGAN").build();
            var matchMono = restClient.match(matchRequestFactory.buildFrom(null, name, LocalDate.of(1975, 1, 1)));

            StepVerifier.create(matchMono)
                .consumeNextWith(match -> Assertions.assertAll(
                    () -> assertThat(match.getMatchedBy()).isEqualTo(OffenderSearchMatchType.ALL_SUPPLIED),
                    () -> assertThat(match.getMatches().size()).isEqualTo(1)
                ))
                .verifyComplete();
        }

        @Test
        public void givenSingleMatchNonExactMatchReturned_whenMatch_thenReturnIt() {
            var name = uk.gov.justice.probation.courtcasematcher.model.domain.Name.builder().forename1("Calvin").surname("HARRIS").build();
            var match = restClient.match(matchRequestFactory.buildFrom(null, name, LocalDate.of(1969, Month.AUGUST, 26)))
                .blockOptional();

            assertThat(match).isPresent();
            assertThat(match.get().getMatchedBy()).isEqualTo(OffenderSearchMatchType.NAME);
            assertThat(match.get().getMatches().size()).isEqualTo(1);
            assertThat(match.get().isExactMatch()).isFalse();
        }

        @Test
        public void givenMultipleMatchesReturned_whenMatch_thenReturnThem() {
            var name = uk.gov.justice.probation.courtcasematcher.model.domain.Name.builder().forename1("John").surname("MARSTON").build();
            var match = restClient.match(matchRequestFactory.buildFrom(null, name, LocalDate.of(1982, 4, 5)))
                .blockOptional();

            assertThat(match).isPresent();
            assertThat(match.get().getMatchedBy()).isEqualTo(OffenderSearchMatchType.ALL_SUPPLIED);
            assertThat(match.get().getMatches().size()).isEqualTo(2);

            var offender1 = match.get().getMatches().get(0).getOffender();
            assertThat(offender1.getOtherIds().getCrn()).isEqualTo("Y346123");
            assertThat(offender1.getOtherIds().getCroNumber()).isEqualTo("2234DEF");
            assertThat(offender1.getOtherIds().getPncNumber()).isEqualTo("BBCD1567");

            var offender2 = match.get().getMatches().get(1).getOffender();
            assertThat(offender2.getOtherIds().getCrn()).isEqualTo("Z346124");
            assertThat(offender2.getOtherIds().getCroNumber()).isEqualTo("3234DEG");
            assertThat(offender2.getOtherIds().getPncNumber()).isEqualTo("CBCD1568");
        }

        @Test
        public void givenNoMatchesReturned_whenMatch_thenReturnEmptyList() {
            var name = uk.gov.justice.probation.courtcasematcher.model.domain.Name.builder().forename1("Juan").surname("MARSTONEZ").build();
            var match = restClient.match(matchRequestFactory.buildFrom(null, name, LocalDate.of(1982, 4, 5)))
                .blockOptional();

            assertThat(match).isPresent();
            assertThat(match.get().getMatchedBy()).isEqualTo(OffenderSearchMatchType.NOTHING);
            assertThat(match.get().getMatches().size()).isEqualTo(0);
        }

        @Test
        public void givenUnexpected500_whenMatch_thenRetryAndError() {
            var name = uk.gov.justice.probation.courtcasematcher.model.domain.Name.builder().forename1("error").surname("error").build();
            var searchResponseMono = restClient.match(matchRequestFactory.buildFrom(null, name, LocalDate.of(1982, 4, 5)));

            StepVerifier.create(searchResponseMono)
                .expectError(reactor.core.Exceptions.retryExhausted("Retries exhausted: 2/2", null).getClass())
                .verify();
        }

        @Test
        public void givenUnexpected404_whenMatch_thenNoRetryButReturnSameError() {
            var name = uk.gov.justice.probation.courtcasematcher.model.domain.Name.builder().forename1("not").surname("found").build();
            var searchResponseMono = restClient.match(matchRequestFactory.buildFrom(null, name, LocalDate.of(1999, 4, 5)));

            StepVerifier.create(searchResponseMono)
                .expectError(WebClientResponseException.NotFound.class)
                .verify();
        }

        @Test
        public void givenUnexpected401_whenMatch_thenNoRetryButReturnSameError() {
            var name = uk.gov.justice.probation.courtcasematcher.model.domain.Name.builder().forename1("unauthorised").surname("unauthorised").build();
            var searchResponseMono = restClient.match(matchRequestFactory.buildFrom(null, name, LocalDate.of(1982, 4, 5)));

            StepVerifier.create(searchResponseMono)
                .expectError(WebClientResponseException.Unauthorized.class)
                .verify();
        }
    }

    @Nested
    class Search {

        private static final String CRN = "X320741";

        @Test
        public void whenSearch_thenReturnIt() {
            var search = restClient.search(CRN).blockOptional();

            assertThat(search).isPresent();
            assertThat(search.get().getSearchResponses()).hasSize(1);

            var target = search.get().getSearchResponses().get(0);
            assertThat(target.getOffenderId()).isEqualTo(2500342345L);
            assertThat(target.getOtherIds().getCrn()).isEqualTo(CRN);
            assertThat(target.getOtherIds().getPncNumber()).isEqualTo("PNC123");

            assertThat(target.getProbationStatusDetail().getStatus()).isEqualTo("CURRENT");
            assertThat(target.getProbationStatusDetail().getInBreach()).isEqualTo(Boolean.FALSE);
            assertThat(target.getProbationStatusDetail().isPreSentenceActivity()).isFalse();
            assertThat(target.getProbationStatusDetail().isAwaitingPsr()).isTrue();
            assertThat(target.getProbationStatusDetail().getPreviouslyKnownTerminationDate()).isEqualTo(LocalDate.of(2013, Month.DECEMBER, 12));
        }

        @Test
        public void givenMultipleResponses_whenSearch_thenReturn() {
            var search = restClient.search("CRNMULTI").blockOptional();

            assertThat(search).isPresent();
            assertThat(search.get().getSearchResponses()).hasSize(2);
        }

        @Test
        public void givenUnexpected500_whenSearch_thenRetryAndError() {
            var searchResponseMono = restClient.search("CRN500");

            StepVerifier.create(searchResponseMono)
                .expectError(reactor.core.Exceptions.retryExhausted("Retries exhausted: 2/2", null).getClass())
                .verify();
        }

        @Test
        public void givenUnexpected401_whenSearch_thenNoRetryButReturnSameError() {
            var searchResponses = restClient.search("CRN401");

            StepVerifier.create(searchResponses)
                .expectError(WebClientResponseException.Unauthorized.class)
                .verify();
        }

        @Test
        public void givenUnexpected404_whenMatch_thenNoRetryButReturnSameError() {
            var searchResponseMono = restClient.search("CRN404");

            StepVerifier.create(searchResponseMono)
                .expectError(WebClientResponseException.NotFound.class)
                .verify();
        }
    }

}
