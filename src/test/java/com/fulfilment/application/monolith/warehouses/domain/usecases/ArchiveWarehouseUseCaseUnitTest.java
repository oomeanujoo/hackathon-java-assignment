package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit test — no @QuarkusTest, no CDI. See CreateWarehouseUseCaseTest for why this style
 * exists alongside ArchiveWarehouseUseCaseTest (which covers real concurrency/DB behavior that a
 * mocked test can't replicate).
 */
public class ArchiveWarehouseUseCaseUnitTest {

  private WarehouseStore warehouseStore;
  private ArchiveWarehouseUseCase archiveWarehouseUseCase;

  @BeforeEach
  public void setup() {
    warehouseStore = mock(WarehouseStore.class);
    archiveWarehouseUseCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  private static Warehouse newWarehouse(String code) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = code;
    return warehouse;
  }

  @Test
  public void testArchiveNonExistentWarehouseThrows() {
    when(warehouseStore.findByBusinessUnitCode("MISSING")).thenReturn(null);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> archiveWarehouseUseCase.archive(newWarehouse("MISSING")));

    assertTrue(exception.getMessage().contains("does not exist"));
  }

  @Test
  public void testArchiveAlreadyArchivedWarehouseThrows() {
    Warehouse existing = newWarehouse("WH-001");
    existing.archivedAt = java.time.LocalDateTime.now();
    when(warehouseStore.findByBusinessUnitCode("WH-001")).thenReturn(existing);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> archiveWarehouseUseCase.archive(newWarehouse("WH-001")));

    assertTrue(exception.getMessage().contains("already archived"));
  }

  @Test
  public void testSuccessfulArchiveSetsTimestampAndCallsUpdate() {
    Warehouse existing = newWarehouse("WH-002");
    when(warehouseStore.findByBusinessUnitCode("WH-002")).thenReturn(existing);

    archiveWarehouseUseCase.archive(newWarehouse("WH-002"));

    assertNotNull(existing.archivedAt);
    verify(warehouseStore, times(1)).update(any(Warehouse.class));
  }
}
