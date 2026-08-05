package com.fulfilment.application.monolith.warehouses.adapters;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.HashMap;
import java.util.Map;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {

  private PostgreSQLContainer<?> postgres;

  @Override
  public Map<String, String> start() {
    postgres = new PostgreSQLContainer<>("postgres:13.3")
        .withDatabaseName("quarkus_test")
        .withUsername("quarkus_test")
        .withPassword("quarkus_test");
    postgres.start();

    Map<String, String> config = new HashMap<>();
    config.put("quarkus.datasource.db-kind", "postgresql");
    config.put("quarkus.datasource.jdbc.url", postgres.getJdbcUrl());
    config.put("quarkus.datasource.username", postgres.getUsername());
    config.put("quarkus.datasource.password", postgres.getPassword());
    config.put("quarkus.datasource.devservices.enabled", "false");
    return config;
  }

  @Override
  public void stop() {
    if (postgres != null) {
      postgres.stop();
    }
  }
}
