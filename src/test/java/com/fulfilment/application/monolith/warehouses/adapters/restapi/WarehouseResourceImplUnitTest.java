package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.WarehousePage;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.beans.Warehouse;
import jakarta.ws.rs.WebApplicationException;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit test — no @QuarkusTest, no CDI. WarehouseResourceImpl uses field injection, so its
 * private fields are set directly via reflection (MockitoAnnotations.openMocks does this for
 * any field annotated @Mock, matching the field's declared name/type). Same rationale as
 * CreateWarehouseUseCaseTest: existing @QuarkusTest-based tests (WarehouseSearchEndpointTest,
 * WarehouseEndpointIT) still cover this class end-to-end over real HTTP; this test exists purely
 * to exercise its own branching logic directly and reliably.
 */
public class WarehouseResourceImplUnitTest {

  @Mock WarehouseRepository warehouseRepository;
  @Mock CreateWarehouseOperation createWarehouseOperation;
  @Mock ArchiveWarehouseOperation archiveWarehouseOperation;
  @Mock ReplaceWarehouseOperation replaceWarehouseOperation;

  private WarehouseResourceImpl resource;

  @BeforeEach
  public void setup() throws Exception {
    MockitoAnnotations.openMocks(this);
    resource = new WarehouseResourceImpl();
    setField("warehouseRepository", warehouseRepository);
    setField("createWarehouseOperation", createWarehouseOperation);
    setField("archiveWarehouseOperation", archiveWarehouseOperation);
    setField("replaceWarehouseOperation", replaceWarehouseOperation);
  }

  private void setField(String name, Object value) throws Exception {
    var field = WarehouseResourceImpl.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(resource, value);
  }

  private static com.fulfilment.application.monolith.warehouses.domain.models.Warehouse domainWarehouse(
      String code, String location, int capacity, int stock) {
    var warehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.businessUnitCode = code;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  private static Warehouse apiWarehouse(String code, String location, Integer capacity, Integer stock) {
    var warehouse = new Warehouse();
    warehouse.setBusinessUnitCode(code);
    warehouse.setLocation(location);
    warehouse.setCapacity(capacity);
    warehouse.setStock(stock);
    return warehouse;
  }

  @Test
  public void testListAllWarehousesUnits() {
    when(warehouseRepository.getAll()).thenReturn(
        List.of(domainWarehouse("WH-001", "AMSTERDAM-001", 50, 10)));

    List<Warehouse> result = resource.listAllWarehousesUnits();

    assertEquals(1, result.size());
    assertEquals("WH-001", result.get(0).getBusinessUnitCode());
  }

  @Test
  public void testCreateANewWarehouseUnitSuccess() {
    Warehouse request = apiWarehouse("WH-002", "AMSTERDAM-001", 50, 10);

    Warehouse response = resource.createANewWarehouseUnit(request);

    assertEquals("WH-002", response.getBusinessUnitCode());
    verify(createWarehouseOperation, times(1)).create(any());
  }

  @Test
  public void testCreateANewWarehouseUnitDefaultsStockToZero() {
    Warehouse request = apiWarehouse("WH-003", "AMSTERDAM-001", 50, null);

    Warehouse response = resource.createANewWarehouseUnit(request);

    assertEquals(0, response.getStock());
  }

  @Test
  public void testCreateANewWarehouseUnitValidationFailureReturns400() {
    doThrow(new IllegalArgumentException("bad input")).when(createWarehouseOperation).create(any());

    WebApplicationException exception = assertThrows(WebApplicationException.class,
        () -> resource.createANewWarehouseUnit(apiWarehouse("WH-004", "AMSTERDAM-001", 50, 10)));

    assertEquals(400, exception.getResponse().getStatus());
  }

  @Test
  public void testGetAWarehouseUnitByIDFound() {
    when(warehouseRepository.findByBusinessUnitCode("WH-005"))
        .thenReturn(domainWarehouse("WH-005", "AMSTERDAM-001", 50, 10));

    Warehouse response = resource.getAWarehouseUnitByID("WH-005");

    assertEquals("WH-005", response.getBusinessUnitCode());
  }

  @Test
  public void testGetAWarehouseUnitByIDNotFoundReturns404() {
    when(warehouseRepository.findByBusinessUnitCode("MISSING")).thenReturn(null);

    WebApplicationException exception = assertThrows(WebApplicationException.class,
        () -> resource.getAWarehouseUnitByID("MISSING"));

    assertEquals(404, exception.getResponse().getStatus());
  }

  @Test
  public void testArchiveAWarehouseUnitByIDSuccess() {
    when(warehouseRepository.findByBusinessUnitCode("WH-006"))
        .thenReturn(domainWarehouse("WH-006", "AMSTERDAM-001", 50, 10));

    resource.archiveAWarehouseUnitByID("WH-006");

    verify(archiveWarehouseOperation, times(1)).archive(any());
  }

  @Test
  public void testArchiveAWarehouseUnitByIDNotFoundReturns404() {
    when(warehouseRepository.findByBusinessUnitCode("MISSING")).thenReturn(null);

    WebApplicationException exception = assertThrows(WebApplicationException.class,
        () -> resource.archiveAWarehouseUnitByID("MISSING"));

    assertEquals(404, exception.getResponse().getStatus());
  }

  @Test
  public void testArchiveAWarehouseUnitByIDValidationFailureReturns400() {
    when(warehouseRepository.findByBusinessUnitCode("WH-007"))
        .thenReturn(domainWarehouse("WH-007", "AMSTERDAM-001", 50, 10));
    doThrow(new IllegalArgumentException("already archived")).when(archiveWarehouseOperation).archive(any());

    WebApplicationException exception = assertThrows(WebApplicationException.class,
        () -> resource.archiveAWarehouseUnitByID("WH-007"));

    assertEquals(400, exception.getResponse().getStatus());
  }

  @Test
  public void testReplaceTheCurrentActiveWarehouseSuccess() {
    when(warehouseRepository.findByBusinessUnitCode("WH-008"))
        .thenReturn(domainWarehouse("WH-008", "AMSTERDAM-002", 60, 20));

    Warehouse response = resource.replaceTheCurrentActiveWarehouse(
        "WH-008", apiWarehouse("WH-008", "AMSTERDAM-002", 60, 20));

    assertEquals("WH-008", response.getBusinessUnitCode());
    verify(replaceWarehouseOperation, times(1)).replace(any());
  }

  @Test
  public void testReplaceTheCurrentActiveWarehouseValidationFailureReturns400() {
    doThrow(new IllegalArgumentException("bad replace")).when(replaceWarehouseOperation).replace(any());

    WebApplicationException exception = assertThrows(WebApplicationException.class,
        () -> resource.replaceTheCurrentActiveWarehouse(
            "WH-009", apiWarehouse("WH-009", "AMSTERDAM-001", 50, 10)));

    assertEquals(400, exception.getResponse().getStatus());
  }

  @Test
  public void testSearchAndFilterWarehouseUnitsDefaultsAndDelegation() {
    when(warehouseRepository.search(eq("AMSTERDAM-001"), eq(50), isNull(), eq("createdAt"), eq(false), eq(0), eq(10)))
        .thenReturn(new WarehousePage(
            List.of(domainWarehouse("WH-010", "AMSTERDAM-001", 80, 10)), 0, 10, 1, 1));

    var result = resource.searchAndFilterWarehouseUnits(
        "AMSTERDAM-001", BigInteger.valueOf(50), null, null, null, null, null);

    assertEquals(1, result.getItems().size());
    assertEquals(1, result.getTotalElements());
  }

  @Test
  public void testSearchAndFilterWarehouseUnitsInvalidSortByReturns400() {
    WebApplicationException exception = assertThrows(WebApplicationException.class,
        () -> resource.searchAndFilterWarehouseUnits(
            null, null, null, "nonsense", null, null, null));

    assertEquals(400, exception.getResponse().getStatus());
  }

  @Test
  public void testSearchAndFilterWarehouseUnitsInvalidSortOrderReturns400() {
    WebApplicationException exception = assertThrows(WebApplicationException.class,
        () -> resource.searchAndFilterWarehouseUnits(
            null, null, null, null, "sideways", null, null));

    assertEquals(400, exception.getResponse().getStatus());
  }

  @Test
  public void testSearchAndFilterWarehouseUnitsClampsPageSizeAboveMax() {
    when(warehouseRepository.search(any(), any(), any(), any(), any(Boolean.class), any(Integer.class), eq(100)))
        .thenReturn(new WarehousePage(List.of(), 0, 100, 0, 0));

    resource.searchAndFilterWarehouseUnits(null, null, null, null, null, null, BigInteger.valueOf(500));

    verify(warehouseRepository, times(1)).search(any(), any(), any(), any(), any(Boolean.class), any(Integer.class), eq(100));
  }

  private static <T> T isNull() {
    return org.mockito.ArgumentMatchers.isNull();
  }
}
