package com.fulfilment.application.monolith.warehouses.domain;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sophisticated Test: Optimistic Locking
 * 
 * Demonstrates handling of concurrent modifications using JPA's @Version field.
 * Optimistic locking prevents lost updates when multiple transactions
 * try to modify the same entity.
 */
@QuarkusTest
public class WarehouseOptimisticLockingTest {

  @Inject
  EntityManager em;

  private Long warehouseId;

  @BeforeEach
  @Transactional
  public void setup() {
    // Clean slate
    em.createQuery("DELETE FROM DbWarehouse").executeUpdate();
    
    // Create a warehouse
    DbWarehouse warehouse = new DbWarehouse();
    warehouse.businessUnitCode = "OPT-LOCK-001";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 100;
    warehouse.stock = 50;
    warehouse.createdAt = java.time.LocalDateTime.now();
    
    em.persist(warehouse);
    em.flush();
    
    warehouseId = warehouse.id;
  }

  /**
   * Test that concurrent modifications trigger OptimisticLockException.
   * 
   * Scenario:
   * 1. Transaction 1 reads warehouse (version = 0)
   * 2. Transaction 2 reads same warehouse (version = 0)
   * 3. Transaction 1 updates and commits (version = 1)
   * 4. Transaction 2 tries to update (still thinks version = 0)
   * 5. OptimisticLockException thrown!
   */
  @Test
  @Transactional
  public void testOptimisticLockingPreventsLostUpdates() {
    // Simulate two separate transactions reading the same warehouse
    DbWarehouse warehouse1 = em.find(DbWarehouse.class, warehouseId);
    DbWarehouse warehouse2 = em.find(DbWarehouse.class, warehouseId);
    
    // Both have the same version initially
    assertEquals(warehouse1.version, warehouse2.version);
    
    // First transaction updates
    updateWarehouseInSeparateTransaction(warehouseId, 80);
    
    // Second transaction tries to update with stale version
    // This should throw OptimisticLockException
    assertThrows(OptimisticLockException.class, () -> {
      warehouse2.stock = 90;
      em.merge(warehouse2);
      em.flush();
    });
  }

  @Test
  @Transactional
  public void testVersionIncrementsonUpdate() {
    DbWarehouse warehouse = em.find(DbWarehouse.class, warehouseId);
    Long initialVersion = warehouse.version;
    
    // Update the warehouse
    warehouse.stock = 60;
    em.merge(warehouse);
    em.flush();
    
    // Version should have incremented
    assertTrue(warehouse.version > initialVersion);
  }

  /**
   * Helper to simulate a separate transaction updating the warehouse.
   */
  @Transactional(TxType.REQUIRES_NEW)
  void updateWarehouseInSeparateTransaction(Long id, int newStock) {
    DbWarehouse warehouse = em.find(DbWarehouse.class, id);
    warehouse.stock = newStock;
    em.merge(warehouse);
    em.flush();
  }
}
