package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class WarehouseRepositoryTest {

  @Inject WarehouseRepository warehouseRepository;

  @Inject EntityManager em;

  @BeforeEach
  @Transactional
  public void setup() {
    em.createQuery("DELETE FROM DbWarehouse").executeUpdate();
  }

  @Test
  @Transactional
  public void testRemoveDeletesExistingWarehouse() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "REMOVE-TEST-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 50;
    warehouse.stock = 10;
    warehouse.createdAt = LocalDateTime.now();
    warehouseRepository.create(warehouse);

    assertNotNull(warehouseRepository.findByBusinessUnitCode("REMOVE-TEST-001"));

    warehouseRepository.remove(warehouse);

    assertNull(warehouseRepository.findByBusinessUnitCode("REMOVE-TEST-001"));
    assertTrue(warehouseRepository.getAll().stream()
        .noneMatch(w -> "REMOVE-TEST-001".equals(w.businessUnitCode)));
  }

  @Test
  @Transactional
  public void testRemoveNonExistentWarehouseThrows() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "DOES-NOT-EXIST";

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> warehouseRepository.remove(warehouse));

    assertEquals(
        "Warehouse with business unit code 'DOES-NOT-EXIST' does not exist",
        exception.getMessage());
  }
}
