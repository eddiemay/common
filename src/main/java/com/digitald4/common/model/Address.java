package com.digitald4.common.model;

import com.digitald4.common.util.JSONUtil;

public class Address {
  private String address;
  private String unit;
  private double latitude;
  private double longitude;

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

  public double getLatitude() {
    return latitude;
  }

  public Address setLatitude(double latitude) {
    this.latitude = latitude;
    return this;
  }

  public double getLongitude() {
    return longitude;
  }

  public Address setLongitude(double longitude) {
    this.longitude = longitude;
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
