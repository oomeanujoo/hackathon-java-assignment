package com.fulfilment.application.monolith.stores;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
public class StoreEventObserverTest {

  @Inject
  StoreEventObserver storeEventObserver;

  @InjectMock
  LegacyStoreManagerGateway legacyGateway;

  private Store testStore;

  @BeforeEach
  @Transactional
  public void setup() {
    Store.deleteAll();
    
    testStore = new Store();
    testStore.name = "Test Store";
    testStore.quantityProductsInStock = 100;
  }

  @Test
  public void testStoreCreatedEventCallsLegacyGateway() throws InterruptedException {
    Mockito.reset(legacyGateway);

    StoreCreatedEvent event = new StoreCreatedEvent(testStore);
    storeEventObserver.onStoreCreated(event);
    
    Thread.sleep(100);
    
    verify(legacyGateway, times(1)).createStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void testStoreUpdatedEventCallsLegacyGateway() throws InterruptedException {
    Mockito.reset(legacyGateway);

    StoreUpdatedEvent event = new StoreUpdatedEvent(testStore);
    storeEventObserver.onStoreUpdated(event);
    
    Thread.sleep(100);
    
    verify(legacyGateway, times(1)).updateStoreOnLegacySystem(any(Store.class));
  }
}
