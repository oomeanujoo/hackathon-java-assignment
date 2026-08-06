package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProductEndpointTest {

  @Test
  public void testCrudProduct() {
    final String path = "product";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    // Delete the TONSTAD:
    given().when().delete(path + "/1").then().statusCode(204);

    // List all, TONSTAD should be missing now:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(not(containsString("TONSTAD")), containsString("KALLAX"), containsString("BESTÅ"));
  }

  @Test
  public void testCreateProduct() {
    given()
        .contentType("application/json")
        .body("{\"name\": \"PRODUCT-CREATE-001\", \"stock\": 5}")
        .when().post("product")
        .then()
        .statusCode(201)
        .body(containsString("PRODUCT-CREATE-001"));
  }

  @Test
  public void testCreateProductWithIdSetReturns422() {
    given()
        .contentType("application/json")
        .body("{\"id\": 999, \"name\": \"PRODUCT-BAD-ID\", \"stock\": 5}")
        .when().post("product")
        .then()
        .statusCode(422);
  }

  @Test
  public void testGetSingleProduct() {
    given().when().get("product/2").then().statusCode(200).body(containsString("KALLAX"));
  }

  @Test
  public void testGetSingleProductNotFound() {
    given().when().get("product/999999").then().statusCode(404);
  }

  @Test
  public void testUpdateProduct() {
    given()
        .contentType("application/json")
        .body("{\"name\": \"KALLAX-RENAMED\", \"stock\": 7}")
        .when().put("product/2")
        .then()
        .statusCode(200)
        .body(containsString("KALLAX-RENAMED"));
  }

  @Test
  public void testUpdateProductWithoutNameReturns422() {
    given()
        .contentType("application/json")
        .body("{\"stock\": 7}")
        .when().put("product/2")
        .then()
        .statusCode(422);
  }

  @Test
  public void testUpdateProductNotFound() {
    given()
        .contentType("application/json")
        .body("{\"name\": \"DOES-NOT-EXIST\", \"stock\": 1}")
        .when().put("product/999999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testDeleteProductNotFound() {
    given().when().delete("product/999999").then().statusCode(404);
  }
}
