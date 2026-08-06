package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit test — no @QuarkusTest, no CDI. WarehouseStore/LocationResolver are mocked with
 * Mockito directly, and CreateWarehouseUseCase is constructed with "new", exercising each
 * validation branch in isolation.
 */
public class CreateWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private LocationResolver locationResolver;
  private CreateWarehouseUseCase createWarehouseUseCase;

  private static final Location AMSTERDAM_001 = new Location("AMSTERDAM-001", 5, 100);

  @BeforeEach
  public void setup() {
    warehouseStore = mock(WarehouseStore.class);
    locationResolver = mock(LocationResolver.class);
    createWarehouseUseCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
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
  public void testDuplicateBusinessUnitCodeThrows() {
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(newWarehouse("WH-001", "AMSTERDAM-001", 10, 5));

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> createWarehouseUseCase.create(newWarehouse("WH-001", "AMSTERDAM-001", 10, 5)));

    assertTrue(exception.getMessage().contains("already exists"));
  }

  @Test
  public void testInvalidLocationThrows() {
    when(warehouseStore.findByBusinessUnitCode(any())).thenReturn(null);
    when(locationResolver.resolveByIdentifier("NOWHERE")).thenReturn(null);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> createWarehouseUseCase.create(newWarehouse("WH-002", "NOWHERE", 10, 5)));

    assertTrue(exception.getMessage().contains("not valid"));
  }

  @Test
  public void testCapacityExceedsLocationMaxThrows() {
    when(warehouseStore.findByBusinessUnitCode(any())).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
    when(warehouseStore.getAll()).thenReturn(List.of());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> createWarehouseUseCase.create(newWarehouse("WH-003", "AMSTERDAM-001", 200, 5)));

    assertTrue(exception.getMessage().contains("exceeds location max capacity"));
  }

  @Test
  public void testStockExceedsCapacityThrows() {
    when(warehouseStore.findByBusinessUnitCode(any())).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
    when(warehouseStore.getAll()).thenReturn(List.of());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> createWarehouseUseCase.create(newWarehouse("WH-004", "AMSTERDAM-001", 50, 60)));

    assertTrue(exception.getMessage().contains("exceeds warehouse capacity"));
  }

  @Test
  public void testExceedsMaxNumberOfWarehousesAtLocationThrows() {
    when(warehouseStore.findByBusinessUnitCode(any())).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(new Location("AMSTERDAM-001", 1, 100));
    when(warehouseStore.getAll()).thenReturn(List.of(newWarehouse("EXISTING-1", "AMSTERDAM-001", 10, 5)));

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> createWarehouseUseCase.create(newWarehouse("WH-005", "AMSTERDAM-001", 10, 5)));

    assertTrue(exception.getMessage().contains("maximum number of warehouses"));
  }

  @Test
  public void testExceedsTotalCapacityAtLocationThrows() {
    when(warehouseStore.findByBusinessUnitCode(any())).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
    when(warehouseStore.getAll()).thenReturn(List.of(newWarehouse("EXISTING-1", "AMSTERDAM-001", 70, 5)));

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> createWarehouseUseCase.create(newWarehouse("WH-006", "AMSTERDAM-001", 40, 5)));

    assertTrue(exception.getMessage().contains("Total warehouse capacity"));
  }

  @Test
  public void testSuccessfulCreateCallsStoreCreate() {
    when(warehouseStore.findByBusinessUnitCode(any())).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM_001);
    when(warehouseStore.getAll()).thenReturn(List.of());

    createWarehouseUseCase.create(newWarehouse("WH-007", "AMSTERDAM-001", 40, 5));

    verify(warehouseStore, times(1)).create(any(Warehouse.class));
  }
}
