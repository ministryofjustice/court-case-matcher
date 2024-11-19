package uk.gov.justice.probation.courtcasematcher.info;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.probation.courtcasematcher.TestConfig;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
public class InfoTest {
    @LocalServerPort
    private int port;

    @BeforeEach
    public void before() {
        TestConfig.configureRestAssuredForIntTest(port);
    }

    @Test
    public void testUp() {
        String response = given()
            .when()
            .get("/info")
            .then()
            .statusCode(200)
            .extract().response().asString();

        assertThatJson(response).node("git").isPresent();
        assertThatJson(response).node("build").isPresent();
    }
}
