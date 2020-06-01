package uk.gov.justice.probation.courtcasematcher.health;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class HealthCheckTest {

    @LocalServerPort
    int port;

    @Before
    public void before() {
        RestAssured.port = port;
        RestAssured.basePath = "/actuator";
    }

    @Test
    public void testUp() {

        String response = given()
                .when()
                .get("health")
                .then()
                .statusCode(200)
                .extract().response().asString();

        assertThatJson(response).node("status").isEqualTo("UP");
    }
}
