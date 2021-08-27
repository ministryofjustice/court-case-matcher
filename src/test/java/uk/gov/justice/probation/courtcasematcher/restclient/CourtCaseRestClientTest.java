package uk.gov.justice.probation.courtcasematcher.restclient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.domain.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.domain.GroupedOffenderMatches;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourtCaseRestClientTest {
    @Mock
    private LegacyCourtCaseRestClient legacyClient;
    @Mock
    private Mono<Void> mono;
    @Mock
    private Mono<CourtCase> courtCaseMono;
    @InjectMocks
    private CourtCaseRestClient client;

    @Test
    public void put_doesTing() {
        final var courtCase = CourtCase.builder().build();
        client.putCourtCase(courtCase);
    }

    @Test
    public void postMatches_delegatesToLegacyClient() {
        final var offenderMatches = GroupedOffenderMatches.builder().build();
        when(legacyClient.postMatches("court code", "case no", offenderMatches)).thenReturn(mono);

        final var actualMono = client.postMatches("court code", "case no", offenderMatches);

        assertThat(actualMono).isEqualTo(mono);
    }

    @Test
    public void getCourtCase_delegatesToLegacyClient() {
        when(legacyClient.getCourtCase("court code", "case no")).thenReturn(courtCaseMono);

        final var actualMono = client.getCourtCase("court code", "case no");

        assertThat(actualMono).isEqualTo(courtCaseMono);
    }
}
