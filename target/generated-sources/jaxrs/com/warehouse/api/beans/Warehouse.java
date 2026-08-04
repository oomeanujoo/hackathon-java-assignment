
package com.warehouse.api.beans;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "businessUnitCode",
    "location",
    "capacity",
    "stock"
})
@Generated("jsonschema2pojo")
public class Warehouse {

    @JsonProperty("id")
    private String id;
    @JsonProperty("businessUnitCode")
    private String businessUnitCode;
    @JsonProperty("location")
    private String location;
    @JsonProperty("capacity")
    private Integer capacity;
    @JsonProperty("stock")
    private Integer stock;

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("businessUnitCode")
    public String getBusinessUnitCode() {
        return businessUnitCode;
    }

    @JsonProperty("businessUnitCode")
    public void setBusinessUnitCode(String businessUnitCode) {
        this.businessUnitCode = businessUnitCode;
    }

    @JsonProperty("location")
    public String getLocation() {
        return location;
    }

    @JsonProperty("location")
    public void setLocation(String location) {
        this.location = location;
    }

    @JsonProperty("capacity")
    public Integer getCapacity() {
        return capacity;
    }

    @JsonProperty("capacity")
    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    @JsonProperty("stock")
    public Integer getStock() {
        return stock;
    }

    @JsonProperty("stock")
    public void setStock(Integer stock) {
        this.stock = stock;
    }

}
