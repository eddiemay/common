package com.digitald4.common.model;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;

import java.time.Instant;

public interface HasModificationTimes {
  @ApiResourceProperty default Long creationTime() {
    return getCreationTime() == null ? null : getCreationTime().toEpochMilli();
  }
  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE) Instant getCreationTime();
  default HasModificationTimes setCreationTime(Instant time) {
    return setCreationTime(time.toEpochMilli());
  }
  HasModificationTimes setCreationTime(long millis);

  @ApiResourceProperty default Long lastModifiedTime() {
    return getLastModifiedTime() == null ? null : getLastModifiedTime().toEpochMilli();
  }
  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE) Instant getLastModifiedTime();
  default HasModificationTimes setLastModifiedTime(Instant time) {
    return setLastModifiedTime(time.toEpochMilli());
  }
  HasModificationTimes setLastModifiedTime(long millis);

  @ApiResourceProperty default Long deletionTime() {
    return getDeletionTime() == null ? null : getDeletionTime().toEpochMilli();
  }
  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE) Instant getDeletionTime();
  default HasModificationTimes setDeletionTime(Instant time) {
    return setDeletionTime(time.toEpochMilli());
  }
  HasModificationTimes setDeletionTime(long millis);
}
