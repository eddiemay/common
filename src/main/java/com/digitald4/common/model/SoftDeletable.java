package com.digitald4.common.model;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import java.time.Instant;

public interface SoftDeletable<ID> extends HasModificationUser<ID> {
  String getDeletionUsername();
  HasModificationUser<ID> setDeletionUsername(String deletionUsername);

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  Instant getDeletionTime();
  HasModificationTimes<ID> setDeletionTime(Instant time);
  @ApiResourceProperty default Long deletionTime() {
    return getDeletionTime() == null ? null : getDeletionTime().toEpochMilli();
  }
  HasModificationTimes<ID> setDeletionTime(long millis);

}
