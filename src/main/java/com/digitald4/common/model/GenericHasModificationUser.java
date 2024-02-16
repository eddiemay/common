package com.digitald4.common.model;

import java.time.Instant;

public class GenericHasModificationUser<ID> extends ModelObject<ID> implements HasModificationUser {

  private Instant creationTime;
  private Instant lastModifiedTime;
  private Instant deletionTime;
  private Long creationUserId;
  private Long lastModifiedUserId;
  private Long deletionUserId;

  @Override
  public Instant getCreationTime() {
    return creationTime;
  }

  @Override
  public HasModificationTimes setCreationTime(long millis) {
    this.creationTime = Instant.ofEpochMilli(millis);
    return this;
  }

  @Override
  public Instant getLastModifiedTime() {
    return lastModifiedTime;
  }

  @Override
  public HasModificationTimes setLastModifiedTime(long millis) {
    this.lastModifiedTime = Instant.ofEpochMilli(millis);
    return this;
  }

  @Override
  public Instant getDeletionTime() {
    return deletionTime;
  }

  @Override
  public HasModificationTimes setDeletionTime(long millis) {
    this.deletionTime = Instant.ofEpochMilli(millis);
    return this;
  }

  @Override
  public Long getCreationUserId() {
    return creationUserId;
  }

  @Override
  public HasModificationUser setCreationUserId(Long creationUserId) {
    this.creationUserId = creationUserId;
    return this;
  }

  @Override
  public Long getLastModifiedUserId() {
    return lastModifiedUserId;
  }

  @Override
  public HasModificationUser setLastModifiedUserId(Long lastModifiedUserId) {
    this.lastModifiedUserId = lastModifiedUserId;
    return this;
  }

  @Override
  public Long getDeletionUserId() {
    return deletionUserId;
  }

  @Override
  public HasModificationUser setDeletionUserId(Long deletionUserId) {
    this.deletionUserId = deletionUserId;
    return this;
  }
}
