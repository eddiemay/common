package com.digitald4.common.model;

public interface HasModificationUser extends HasModificationTimes {
  Long getCreationUserId();
  HasModificationUser setCreationUserId(Long userId);
  Long getLastModifiedUserId();
  HasModificationUser setLastModifiedUserId(Long userId);
  Long getDeletionUserId();
  HasModificationUser setDeletionUserId(Long userId);
}
