package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StoreEndpointTest {

  @Test
  public void testListStores() {
    given()
        .when()
        .get("store")
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));
  }

  @Test
  public void testGetSingleStore() {
    given().when().get("store/2").then().statusCode(200).body(containsString("KALLAX"));
  }

  @Test
  public void testGetSingleStoreNotFound() {
    given().when().get("store/999999").then().statusCode(404);
  }

  @Test
  public void testCreateStoreWithIdSetReturns422() {
    given()
        .contentType("application/json")
        .body("{\"id\": 999, \"name\": \"STORE-BAD-ID\", \"quantityProductsInStock\": 5}")
        .when().post("store")
        .then()
        .statusCode(422);
  }

  @Test
  public void testUpdateStore() {
    given()
        .contentType("application/json")
        .body("{\"name\": \"KALLAX-STORE-RENAMED\", \"quantityProductsInStock\": 7}")
        .when().put("store/2")
        .then()
        .statusCode(200)
        .body(containsString("KALLAX-STORE-RENAMED"));
  }

  @Test
  public void testUpdateStoreWithoutNameReturns422() {
    given()
        .contentType("application/json")
        .body("{\"quantityProductsInStock\": 7}")
        .when().put("store/2")
        .then()
        .statusCode(422);
  }

  @Test
  public void testUpdateStoreNotFound() {
    given()
        .contentType("application/json")
        .body("{\"name\": \"DOES-NOT-EXIST\", \"quantityProductsInStock\": 1}")
        .when().put("store/999999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testPatchStore() {
    given()
        .contentType("application/json")
        .body("{\"name\": \"KALLAX-PATCHED\", \"quantityProductsInStock\": 3}")
        .when().patch("store/2")
        .then()
        .statusCode(200)
        .body(containsString("KALLAX-PATCHED"));
  }

  @Test
  public void testPatchStoreWithoutNameReturns422() {
    given()
        .contentType("application/json")
        .body("{\"quantityProductsInStock\": 3}")
        .when().patch("store/2")
        .then()
        .statusCode(422);
  }

  @Test
  public void testPatchStoreNotFound() {
    given()
        .contentType("application/json")
        .body("{\"name\": \"DOES-NOT-EXIST\", \"quantityProductsInStock\": 1}")
        .when().patch("store/999999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testDeleteStoreNotFound() {
    given().when().delete("store/999999").then().statusCode(404);
  }
}
