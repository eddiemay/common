package com.digitald4.common.model;

public class GeneralData extends ModelObject<Long> {
  private long groupId;
  private int inGroupId;
  private String name;
  private double rank;
  private boolean active;
  private String description;
  private String data;

  @Override
  public Long getId() {
    return super.getId();
  }

  public GeneralData setId(long id) {
    super.setId(id);
    return this;
  }

  public long getGroupId() {
    return groupId;
  }

  public GeneralData setGroupId(long groupId) {
    this.groupId = groupId;
    return this;
  }

  public int getInGroupId() {
    return inGroupId;
  }

  public GeneralData setInGroupId(int inGroupId) {
    this.inGroupId = inGroupId;
    return this;
  }

  public String getName() {
    return name;
  }

  public GeneralData setName(String name) {
    this.name = name;
    return this;
  }

  public double getRank() {
    return rank;
  }

  public GeneralData setRank(double rank) {
    this.rank = rank;
    return this;
  }

  public boolean isActive() {
    return active;
  }

  public GeneralData setActive(boolean active) {
    this.active = active;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public GeneralData setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getData() {
    return data;
  }

  public GeneralData setData(String data) {
    this.data = data;
    return this;
  }
}
