package com.digitald4.common.model;

import java.time.Instant;

public interface HasModificationTimes {
  Instant getCreationTime();
  default HasModificationTimes setCreationTime(Instant time) {
    return setCreationTime(time.toEpochMilli());
  }
  HasModificationTimes setCreationTime(long millis);

  Instant getLastModifiedTime();
  default HasModificationTimes setLastModifiedTime(Instant time) {
    return setLastModifiedTime(time.toEpochMilli());
  }
  HasModificationTimes setLastModifiedTime(long millis);

  Instant getDeletionTime();
  default HasModificationTimes setDeletionTime(Instant time) {
    return setDeletionTime(time.toEpochMilli());
  }
  HasModificationTimes setDeletionTime(long millis);
}
