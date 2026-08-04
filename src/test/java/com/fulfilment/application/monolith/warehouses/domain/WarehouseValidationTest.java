package com.fulfilment.application.monolith.warehouses.domain;

import com.fulfilment.application.monolith.location.LocationGateway;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.usecases.CreateWarehouseUseCase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sophisticated Test: Parameterized Testing
 * 
 * Demonstrates systematic edge case testing using @ParameterizedTest.
 * Instead of writing repetitive test methods, we define scenarios as data
 * and test them all with one test method.
 */
@QuarkusTest
public class WarehouseValidationTest {

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
   * Test invalid capacity scenarios using parameterized tests.
   * Each scenario is defined as: capacity, stock, expectedErrorSubstring
   */
  @ParameterizedTest
  @MethodSource("invalidCapacityScenarios")
  public void testInvalidCapacityScenarios(int capacity, int stock, String location, String expectedError) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "TEST-" + System.currentTimeMillis();
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      createWarehouseUseCase.create(warehouse);
    });

    assertTrue(exception.getMessage().contains(expectedError),
        "Expected error to contain: " + expectedError + " but got: " + exception.getMessage());
  }

  /**
   * Data source for parameterized test.
   * Tests various edge cases: capacity too high, stock exceeds capacity, etc.
   */
  private static Stream<Arguments> invalidCapacityScenarios() {
    return Stream.of(
        // capacity, stock, location, expectedError
        Arguments.of(150, 10, "ZWOLLE-001", "exceeds location max capacity"), // capacity > location max (40)
        Arguments.of(30, 50, "ZWOLLE-001", "exceeds warehouse capacity"),     // stock > capacity
        Arguments.of(200, 10, "AMSTERDAM-001", "exceeds location max capacity"), // capacity > 100
        Arguments.of(50, 60, "AMSTERDAM-001", "exceeds warehouse capacity")      // stock > capacity
    );
  }

  /**
   * Test invalid location scenarios.
   */
  @ParameterizedTest
  @MethodSource("invalidLocationScenarios")
  public void testInvalidLocationScenarios(String location, String expectedError) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "TEST-LOC-" + System.currentTimeMillis();
    warehouse.location = location;
    warehouse.capacity = 10;
    warehouse.stock = 5;

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      createWarehouseUseCase.create(warehouse);
    });

    assertTrue(exception.getMessage().contains(expectedError));
  }

  private static Stream<Arguments> invalidLocationScenarios() {
    return Stream.of(
        Arguments.of("INVALID-LOC", "not valid"),
        Arguments.of("NONEXISTENT-001", "not valid"),
        Arguments.of("", "not valid")
    );
  }

  /**
   * Test business unit code uniqueness.
   */
  @ParameterizedTest
  @MethodSource("duplicateBusinessCodeScenarios")
  @Transactional
  public void testDuplicateBusinessUnitCode(String code) {
    // Create first warehouse
    Warehouse warehouse1 = new Warehouse();
    warehouse1.businessUnitCode = code;
    warehouse1.location = "ZWOLLE-001";
    warehouse1.capacity = 10;
    warehouse1.stock = 5;
    
    createWarehouseUseCase.create(warehouse1);

    // Try to create second warehouse with same code
    Warehouse warehouse2 = new Warehouse();
    warehouse2.businessUnitCode = code;  // Same code!
    warehouse2.location = "AMSTERDAM-001";
    warehouse2.capacity = 20;
    warehouse2.stock = 10;

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      createWarehouseUseCase.create(warehouse2);
    });

    assertTrue(exception.getMessage().contains("already exists"));
  }

  private static Stream<Arguments> duplicateBusinessCodeScenarios() {
    return Stream.of(
        Arguments.of("DUP-CODE-001"),
        Arguments.of("DUP-CODE-002"),
        Arguments.of("DUP-CODE-003")
    );
  }
}
