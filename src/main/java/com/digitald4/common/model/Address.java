package com.digitald4.common.model;

import com.digitald4.common.util.JSONUtil;

public class Address {
  private String address;
  private String unit;
  private Double latitude;
  private Double longitude;

  public String getAddress() {
    return address;
  }

  public Address setAddress(String address) {
    this.address = address;
    return this;
  }

  public String getUnit() {
    return unit;
  }

  public Address setUnit(String unit) {
    this.unit = unit;
    return this;
  }

  public Double getLatitude() {
    return latitude;
  }

  public Address setLatitude(Double latitude) {
    this.latitude = latitude == null || latitude == 0 ? null : latitude;
    return this;
  }

  public Double getLongitude() {
    return longitude;
  }

  public Address setLongitude(Double longitude) {
    this.longitude = longitude == null || longitude == 0 ? null : longitude;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Address && toString().equals(obj.toString());
  }

  @Override
  public String toString() {
    return JSONUtil.toJSON(this).toString();
  }
}
