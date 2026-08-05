package com.warehouse.api;

import com.warehouse.api.beans.Warehouse;
import com.warehouse.api.beans.WarehouseSearchResult;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import java.math.BigInteger;
import java.util.List;

/**
 * A JAX-RS interface. An implementation of this interface must be provided.
 */
@Path("/warehouse")
public interface WarehouseResource {
  @GET
  @Produces("application/json")
  List<Warehouse> listAllWarehousesUnits();

  @POST
  @Produces("application/json")
  @Consumes("application/json")
  Warehouse createANewWarehouseUnit(@NotNull Warehouse data);

  @Path("/{id}")
  @GET
  @Produces("application/json")
  Warehouse getAWarehouseUnitByID(@PathParam("id") String id);

  @Path("/{id}")
  @DELETE
  void archiveAWarehouseUnitByID(@PathParam("id") String id);

  /**
   * <p>
   * Lists active (non-archived) warehouse units, optionally filtered by location
   * and/or capacity range. All parameters are optional. When more than one filter
   * is given, a warehouse must match all of them (AND logic). Results are
   * paginated and sortable.
   * </p>
   * 
   */
  @Path("/search")
  @GET
  @Produces("application/json")
  WarehouseSearchResult searchAndFilterWarehouseUnits(@QueryParam("location") String location,
      @QueryParam("minCapacity") BigInteger minCapacity, @QueryParam("maxCapacity") BigInteger maxCapacity,
      @QueryParam("sortBy") @DefaultValue("createdAt") String sortBy,
      @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder,
      @QueryParam("page") @DefaultValue("0") BigInteger page,
      @QueryParam("pageSize") @DefaultValue("10") BigInteger pageSize);

  /**
   * <p>
   * Replaces the current active Warehouse identified by
   * <code>businessUnitCode</code> unit by a new Warehouse provided in the request
   * body A Warehouse can be replaced by another Warehouse with the same Business
   * Unit Code. That means that the previous Warehouse will be archived and the
   * new Warehouse will be created assuming its place.
   * </p>
   * 
   */
  @Path("/{businessUnitCode}/replacement")
  @POST
  @Produces("application/json")
  @Consumes("application/json")
  Warehouse replaceTheCurrentActiveWarehouse(@PathParam("businessUnitCode") String businessUnitCode,
      @NotNull Warehouse data);
}
