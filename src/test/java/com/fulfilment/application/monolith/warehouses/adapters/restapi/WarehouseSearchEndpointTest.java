package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WarehouseSearchEndpointTest {

  @Inject EntityManager em;

  @BeforeEach
  @Transactional
  public void setup() {
    em.createQuery("DELETE FROM DbWarehouse").executeUpdate();
  }

  @Transactional(TxType.REQUIRES_NEW)
  void seedWarehouse(
      String businessUnitCode, String location, int capacity, int stock, LocalDateTime archivedAt) {
    DbWarehouse warehouse = new DbWarehouse();
    warehouse.businessUnitCode = businessUnitCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    warehouse.createdAt = LocalDateTime.now();
    warehouse.archivedAt = archivedAt;
    em.persist(warehouse);
  }

  @Test
  public void testSearchExcludesArchivedWarehouses() {
    seedWarehouse("SEARCH-ACTIVE-001", "AMSTERDAM-001", 80, 10, null);
    seedWarehouse("SEARCH-ARCHIVED-001", "AMSTERDAM-001", 90, 10, LocalDateTime.now());

    given()
        .when()
        .get("warehouse/search")
        .then()
        .statusCode(200)
        .body(containsString("SEARCH-ACTIVE-001"), not(containsString("SEARCH-ARCHIVED-001")));
  }

  @Test
  public void testSearchFiltersCombineWithAndLogic() {
    seedWarehouse("SEARCH-MATCH-001", "AMSTERDAM-001", 80, 10, null);
    seedWarehouse("SEARCH-WRONG-LOCATION-001", "ZWOLLE-001", 80, 10, null);
    seedWarehouse("SEARCH-TOO-SMALL-001", "AMSTERDAM-001", 20, 10, null);

    given()
        .when()
        .get("warehouse/search?location=AMSTERDAM-001&minCapacity=50")
        .then()
        .statusCode(200)
        .body(
            containsString("SEARCH-MATCH-001"),
            not(containsString("SEARCH-WRONG-LOCATION-001")),
            not(containsString("SEARCH-TOO-SMALL-001")));
  }

  @Test
  public void testSearchSortingAndPagination() {
    seedWarehouse("SEARCH-PAGE-BIG", "AMSTERDAM-001", 80, 10, null);
    seedWarehouse("SEARCH-PAGE-MID", "AMSTERDAM-001", 60, 10, null);
    seedWarehouse("SEARCH-PAGE-SMALL", "AMSTERDAM-001", 40, 10, null);

    given()
        .when()
        .get(
            "warehouse/search?location=AMSTERDAM-001&sortBy=capacity&sortOrder=desc&page=0&pageSize=2")
        .then()
        .statusCode(200)
        .body("totalElements", org.hamcrest.Matchers.equalTo(3))
        .body("totalPages", org.hamcrest.Matchers.equalTo(2))
        .body("items[0].businessUnitCode", org.hamcrest.Matchers.equalTo("SEARCH-PAGE-BIG"))
        .body("items[1].businessUnitCode", org.hamcrest.Matchers.equalTo("SEARCH-PAGE-MID"));

    given()
        .when()
        .get(
            "warehouse/search?location=AMSTERDAM-001&sortBy=capacity&sortOrder=desc&page=1&pageSize=2")
        .then()
        .statusCode(200)
        .body("items.size()", org.hamcrest.Matchers.equalTo(1))
        .body("items[0].businessUnitCode", org.hamcrest.Matchers.equalTo("SEARCH-PAGE-SMALL"));
  }

  @Test
  public void testSearchRejectsUnknownSortField() {
    given()
        .when()
        .get("warehouse/search?sortBy=notARealField")
        .then()
        .statusCode(400);
  }

  @Test
  public void testSearchRejectsUnknownSortOrder() {
    given()
        .when()
        .get("warehouse/search?sortOrder=sideways")
        .then()
        .statusCode(400);
  }
}
