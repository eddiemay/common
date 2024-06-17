package com.digitald4.common.model;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;

import java.time.Instant;

public interface HasModificationTimes {
  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE) Instant getCreationTime();
  HasModificationTimes setCreationTime(Instant time);
  @ApiResourceProperty default Long creationTime() {
    return getCreationTime() == null ? null : getCreationTime().toEpochMilli();
  }
  HasModificationTimes setCreationTime(long millis);
  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE) Instant getLastModifiedTime();
  HasModificationTimes setLastModifiedTime(Instant time);
  @ApiResourceProperty default Long lastModifiedTime() {
    return getLastModifiedTime() == null ? null : getLastModifiedTime().toEpochMilli();
  }
  HasModificationTimes setLastModifiedTime(long millis);

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE) Instant getDeletionTime();
  HasModificationTimes setDeletionTime(Instant time);
  @ApiResourceProperty default Long deletionTime() {
    return getDeletionTime() == null ? null : getDeletionTime().toEpochMilli();
  }
  default HasModificationTimes setDeletionTime(long millis) {
    return setDeletionTime(Instant.ofEpochMilli(millis));
  }
}
