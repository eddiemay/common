package com.digitald4.common.model;

public interface HasModificationUser extends HasModificationTimes {
  long getCreationUserId();
  HasModificationTimes setCreationUserId(long userId);
  long getLastModifiedUserId();
  HasModificationTimes setLastModifiedUserId(long userId);
  long getDeletionUserId();
  HasModificationTimes setDeletionUserId(long userId);
}
