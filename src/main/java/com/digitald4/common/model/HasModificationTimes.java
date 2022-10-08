package com.digitald4.common.model;

import org.joda.time.DateTime;

public interface HasModificationTimes {
  DateTime getCreationTime();
  HasModificationTimes setCreationTime(DateTime time);
  DateTime getLastModifiedTime();
  HasModificationTimes setLastModifiedTime(DateTime time);
  DateTime getDeletionTime();
  HasModificationTimes setDeletionTime(DateTime time);
}
