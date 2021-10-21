package com.digitald4.common.model;

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
}
