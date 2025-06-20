package com.digitald4.common.model;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;

import java.time.Instant;

public class ModelObjectModTime<ID> extends ModelObject<ID> implements HasModificationTimes<ID> {
  private Instant creationTime;
  private Instant lastModifiedTime;
  private Instant deletionTime;

  @Override
  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  public Instant getCreationTime() {
    return creationTime;
  }

  @Override
  public HasModificationTimes<ID> setCreationTime(Instant creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @ApiResourceProperty
  public Long creationTime() {
    return getCreationTime() == null ? null : getCreationTime().toEpochMilli();
  }

  @Override
  public HasModificationTimes<ID> setCreationTime(long millis) {
    return setCreationTime(Instant.ofEpochMilli(millis));
  }

  @Override
  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  public Instant getLastModifiedTime() {
    return lastModifiedTime;
  }

  @Override
  public HasModificationTimes<ID> setLastModifiedTime(Instant lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
    return this;
  }

  @ApiResourceProperty
  public Long lastModifiedTime() {
    return getLastModifiedTime() == null ? null : getLastModifiedTime().toEpochMilli();
  }

  @Override
  public HasModificationTimes<ID> setLastModifiedTime(long millis) {
    return setLastModifiedTime(Instant.ofEpochMilli(millis));
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  public Instant getDeletionTime() {
    return deletionTime;
  }

  public HasModificationTimes<ID> setDeletionTime(Instant deletionTime) {
    this.deletionTime = deletionTime;
    return this;
  }

  @ApiResourceProperty
  public Long deletionTime() {
    return getDeletionTime() == null ? null : getDeletionTime().toEpochMilli();
  }

  public HasModificationTimes<ID> setDeletionTime(long millis) {
    return setDeletionTime(Instant.ofEpochMilli(millis));
  }
}
