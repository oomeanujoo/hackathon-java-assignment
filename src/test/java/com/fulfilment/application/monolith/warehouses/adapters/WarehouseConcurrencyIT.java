package com.fulfilment.application.monolith.warehouses.adapters;

import com.fulfilment.application.monolith.location.LocationGateway;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.usecases.CreateWarehouseUseCase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sophisticated Test: Concurrency Integration Test
 * 
 * Tests race conditions and thread safety by simulating concurrent requests.
 * This test is NOT explicitly mentioned in documentation - candidates discover it!
 * 
 * Key Concepts:
 * - ExecutorService for concurrent execution
 * - CountDownLatch for synchronization
 * - Database constraints under load
 * - Handling concurrent duplicates
 */
@QuarkusTest
public class WarehouseConcurrencyIT {

  @Inject
  WarehouseRepository warehouseRepository;

  @Inject
  LocationGateway locationResolver;

  private CreateWarehouseUseCase createWarehouseUseCase;

  @BeforeEach
  @Transactional
  public void setup() {
    createWarehouseUseCase = new CreateWarehouseUseCase(warehouseRepository, locationResolver);
  }

  /**
   * Test concurrent creation of warehouses with unique codes.
   * All should succeed.
   */
  @Test
  public void testConcurrentWarehouseCreationWithUniqueCodesSucceeds() throws InterruptedException {
    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    
    List<Future<Boolean>> futures = new ArrayList<>();
    
    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      Future<Boolean> future = executor.submit(() -> {
        try {
          Warehouse warehouse = new Warehouse();
          warehouse.businessUnitCode = "CONCURRENT-" + index;
          warehouse.location = "AMSTERDAM-001";
          warehouse.capacity = 50;
          warehouse.stock = 10;
          
          createWarehouseUseCase.create(warehouse);
          return true;
        } catch (Exception e) {
          return false;
        } finally {
          latch.countDown();
        }
      });
      futures.add(future);
    }
    
    latch.await(10, TimeUnit.SECONDS);
    executor.shutdown();
    
    // All should succeed since codes are unique
    long successCount = futures.stream().filter(f -> {
      try {
        return f.get();
      } catch (Exception e) {
        return false;
      }
    }).count();
    
    assertEquals(threadCount, successCount, "All concurrent creations with unique codes should succeed");
  }

  /**
   * Test concurrent creation of warehouses with SAME code.
   * Only one should succeed, others should fail with duplicate error.
   */
  @Test
  public void testConcurrentWarehouseCreationWithDuplicateCodeFails() throws InterruptedException {
    int threadCount = 5;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);
    
    String duplicateCode = "DUPLICATE-CODE-" + System.currentTimeMillis();
    
    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        try {
          Warehouse warehouse = new Warehouse();
          warehouse.businessUnitCode = duplicateCode;  // Same code for all!
          warehouse.location = "ZWOLLE-001";
          warehouse.capacity = 30;
          warehouse.stock = 5;
          
          createWarehouseUseCase.create(warehouse);
          successCount.incrementAndGet();
        } catch (Exception e) {
          // Expected: duplicate key or already exists error
          failureCount.incrementAndGet();
        } finally {
          latch.countDown();
        }
      });
    }
    
    latch.await(10, TimeUnit.SECONDS);
    executor.shutdown();
    
    // Only one should succeed
    assertEquals(1, successCount.get(), "Only one warehouse with duplicate code should be created");
    assertEquals(threadCount - 1, failureCount.get(), "Other attempts should fail");
  }

  /**
   * Test concurrent reads don't block each other (read scalability).
   */
  @Test
  @Transactional
  public void testConcurrentReadsAreNonBlocking() throws InterruptedException {
    // Create a warehouse first
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "READ-TEST-001";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 100;
    warehouse.stock = 50;
    createWarehouseUseCase.create(warehouse);
    
    int readThreadCount = 20;
    ExecutorService executor = Executors.newFixedThreadPool(readThreadCount);
    CountDownLatch latch = new CountDownLatch(readThreadCount);
    
    AtomicInteger successfulReads = new AtomicInteger(0);
    
    for (int i = 0; i < readThreadCount; i++) {
      executor.submit(() -> {
        try {
          Warehouse found = warehouseRepository.findByBusinessUnitCode("READ-TEST-001");
          if (found != null) {
            successfulReads.incrementAndGet();
          }
        } finally {
          latch.countDown();
        }
      });
    }
    
    latch.await(10, TimeUnit.SECONDS);
    executor.shutdown();
    
    // All reads should succeed
    assertEquals(readThreadCount, successfulReads.get(), "All concurrent reads should succeed");
  }
}
