package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class StoreEventObserver {

  private static final Logger LOGGER = Logger.getLogger(StoreEventObserver.class.getName());

  @Inject 
  LegacyStoreManagerGateway legacyStoreManagerGateway;

  public void onStoreCreated(@ObservesAsync StoreCreatedEvent event) {
    LOGGER.info("Store created event received, syncing with legacy system: " + event.getStore().id);
    legacyStoreManagerGateway.createStoreOnLegacySystem(event.getStore());
  }

  public void onStoreUpdated(@ObservesAsync StoreUpdatedEvent event) {
    LOGGER.info("Store updated event received, syncing with legacy system: " + event.getStore().id);
    legacyStoreManagerGateway.updateStoreOnLegacySystem(event.getStore());
  }
}
