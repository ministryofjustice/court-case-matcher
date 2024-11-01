package uk.gov.justice.probation.courtcasematcher.health;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.TestConfig;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.application.healthchecks.SqsCheck;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
public class HealthCheckTest {

    @Autowired
    private SqsCheck sqsCheck;

    @LocalServerPort
    private int port;

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @BeforeEach
    public void before() {
        TestConfig.configureRestAssuredForIntTest(port);
        RestAssured.basePath = "/actuator";
    }

    @Test
    public void testUp() {
        when(sqsCheck.getHealth(any(Boolean.class))).thenReturn(Mono.just(Health.up().build()));

        String response = given()
            .when()
            .get("/health")
            .then()
            .statusCode(200)
            .extract().response().asString();

        assertThatJson(response).node("status").isEqualTo("UP");
        assertThatJson(response).node("components.sqsCheck.status").isEqualTo("UP");
        assertThatJson(response).node("components.offenderSearch.status").isEqualTo("UP");
        assertThatJson(response).node("components.courtCaseService.status").isEqualTo("UP");
        assertThatJson(response).node("components.nomisAuth.status").isEqualTo("UP");
    }

    @Test
    public void whenSQSDown_thenDownWithStatus503() {
        when(sqsCheck.getHealth(any(Boolean.class))).thenReturn(Mono.just(Health.down().build()));
        String response = given()
            .when()
            .get("/health")
            .then()
            .statusCode(503)
            .extract().response().asString();

        assertThatJson(response).node("status").isEqualTo("DOWN");
        assertThatJson(response).node("components.sqsCheck.status").isEqualTo("DOWN");
        assertThatJson(response).node("components.offenderSearch.status").isEqualTo("UP");
        assertThatJson(response).node("components.courtCaseService.status").isEqualTo("UP");
        assertThatJson(response).node("components.nomisAuth.status").isEqualTo("UP");
    }

    @Test
    public void healthPingIsUp() {
        String response = given()
            .when()
            .get("/health/ping")
            .then()
            .statusCode(200)
            .extract().response().asString();

        assertThatJson(response).node("status").isEqualTo("UP");
    }

    @Test
    public void healthReadinessIsUp() {
        String response = given()
            .when()
            .get("/health/readiness")
            .then()
            .statusCode(200)
            .extract().response().asString();

        assertThatJson(response).node("status").isEqualTo("UP");
    }

    @Test
    public void healthLivenessIsUp() {
        String response = given()
            .when()
            .get("/health/liveness")
            .then()
            .statusCode(200)
            .extract().response().asString();

        assertThatJson(response).node("status").isEqualTo("UP");
    }
}
