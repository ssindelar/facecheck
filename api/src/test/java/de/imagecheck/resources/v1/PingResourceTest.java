package de.imagecheck.resources.v1;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

class PingResourceTest {

	@Test
	void test() {
		given().when().get("/rest/v1/ping").then().statusCode(200).body(is("pong"));
	}

}
