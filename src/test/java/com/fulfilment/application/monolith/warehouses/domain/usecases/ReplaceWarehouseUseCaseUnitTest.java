package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit test — no @QuarkusTest, no CDI. See CreateWarehouseUseCaseTest for why this style
 * exists alongside ReplaceWarehouseUseCaseTest (which covers real DB behavior that a mocked test
 * can't replicate).
 */
public class ReplaceWarehouseUseCaseUnitTest {

  private WarehouseStore warehouseStore;
  private LocationResolver locationResolver;
  private ReplaceWarehouseUseCase replaceWarehouseUseCase;

  private static final Location AMSTERDAM_001 = new Location("AMSTERDAM-001", 5, 100);

  @BeforeEach
  public void setup() {
    warehouseStore = mock(WarehouseStore.class);
    locationResolver = mock(LocationResolver.class);
    replaceWarehouseUseCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);
  }

  private static Warehouse newWarehouse(String code, String location, int capacity, int stock) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = code;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  @Test
  public void testReplaceNonExistentWarehouseThrows() {
    when(warehouseStore.findByBusinessUnitCode("MISSING")).thenReturn(null);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> replaceWarehouseUseCase.replace(newWarehouse("MISSING", "AMSTERDAM-001", 10, 5)));

    assertTrue(exception.getMessage().contains("does not exist"));
  }

  @Test
  public void testReplaceArchivedWarehouseThrows() {
    Warehouse existing = newWarehouse("WH-001", "AMSTERDAM-001", 10, 5);
    existing.archivedAt = java.time.LocalDateTime.now();
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(existing);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> replaceWarehouseUseCase.replace(newWarehouse("WH-001", "AMSTERDAM-001", 10, 5)));

    assertTrue(exception.getMessage().contains("archived and cannot be replaced"));
  }

  @Test
  public void testReplaceWithInvalidLocationThrows() {
    when(warehouseStore.findByBusinessUnitCode("WH-002")).thenReturn(newWarehouse("WH-002", "AMSTERDAM-001", 10, 5));
    when(locationResolver.resolveByIdentifier("NOWHERE")).thenReturn(null);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> replaceWarehouseUseCase.replace(newWarehouse("WH-002", "NOWHERE", 10, 5)));

    assertTrue(exception.getMessage().contains("not valid"));
  }

  @Test
  public void testReplaceCapacityExceedsLocationMaxThrows() {
    when(warehouseStore.findByBusinessUnitCode("WH-003")).thenReturn(newWarehouse("WH-003", "AMSTERDAM-001", 10, 5));
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
    when(warehouseStore.getAll()).thenReturn(List.of());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> replaceWarehouseUseCase.replace(newWarehouse("WH-003", "AMSTERDAM-001", 200, 5)));

    assertTrue(exception.getMessage().contains("exceeds location max capacity"));
  }

  @Test
  public void testReplaceStockExceedsCapacityThrows() {
    when(warehouseStore.findByBusinessUnitCode("WH-004")).thenReturn(newWarehouse("WH-004", "AMSTERDAM-001", 10, 5));
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
    when(warehouseStore.getAll()).thenReturn(List.of());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> replaceWarehouseUseCase.replace(newWarehouse("WH-004", "AMSTERDAM-001", 50, 60)));

    assertTrue(exception.getMessage().contains("exceeds warehouse capacity"));
  }

  @Test
  public void testReplaceExceedsMaxNumberOfWarehousesAtLocationThrows() {
    when(warehouseStore.findByBusinessUnitCode("WH-005")).thenReturn(newWarehouse("WH-005", "AMSTERDAM-001", 10, 5));
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(new Location("AMSTERDAM-001", 1, 100));
    when(warehouseStore.getAll()).thenReturn(List.of(newWarehouse("OTHER-WH", "AMSTERDAM-001", 10, 5)));

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> replaceWarehouseUseCase.replace(newWarehouse("WH-005", "AMSTERDAM-001", 10, 5)));

    assertTrue(exception.getMessage().contains("maximum number of warehouses"));
  }

  @Test
  public void testReplaceExceedsTotalCapacityAtLocationThrows() {
    when(warehouseStore.findByBusinessUnitCode("WH-006")).thenReturn(newWarehouse("WH-006", "AMSTERDAM-001", 10, 5));
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
    when(warehouseStore.getAll()).thenReturn(List.of(newWarehouse("OTHER-WH", "AMSTERDAM-001", 70, 5)));

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> replaceWarehouseUseCase.replace(newWarehouse("WH-006", "AMSTERDAM-001", 40, 5)));

    assertTrue(exception.getMessage().contains("Total warehouse capacity"));
  }

  @Test
  public void testSuccessfulReplaceUpdatesFieldsAndCallsUpdate() {
    Warehouse existing = newWarehouse("WH-007", "ZWOLLE-001", 10, 5);
    when(warehouseStore.findByBusinessUnitCode("WH-007")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
    when(warehouseStore.getAll()).thenReturn(List.of());

    replaceWarehouseUseCase.replace(newWarehouse("WH-007", "AMSTERDAM-001", 40, 20));

    assertEquals("AMSTERDAM-001", existing.location);
    assertEquals(40, existing.capacity);
    assertEquals(20, existing.stock);
    verify(warehouseStore, times(1)).update(existing);
  }
}
