package com.digitald4.common.model;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;

import java.time.Instant;

public interface HasModificationTimes<ID> {
  ID getId();

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE) Instant getCreationTime();
  HasModificationTimes<ID> setCreationTime(Instant time);
  @ApiResourceProperty default Long creationTime() {
    return getCreationTime() == null ? null : getCreationTime().toEpochMilli();
  }
  HasModificationTimes<ID> setCreationTime(long millis);

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE) Instant getLastModifiedTime();
  HasModificationTimes<ID> setLastModifiedTime(Instant time);
  @ApiResourceProperty default Long lastModifiedTime() {
    return getLastModifiedTime() == null ? null : getLastModifiedTime().toEpochMilli();
  }
  HasModificationTimes<ID> setLastModifiedTime(long millis);
}
