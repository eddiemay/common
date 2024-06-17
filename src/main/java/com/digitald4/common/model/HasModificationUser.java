package com.digitald4.common.model;

public interface HasModificationUser extends HasModificationTimes {
  @Deprecated Long getCreationUserId();
  @Deprecated HasModificationUser setCreationUserId(Long userId);
  @Deprecated Long getLastModifiedUserId();
  @Deprecated HasModificationUser setLastModifiedUserId(Long userId);

  String getCreationUsername();
  HasModificationUser setCreationUsername(String creationUsername);
  String getLastModifiedUsername();
  HasModificationUser setLastModifiedUsername(String lastModifiedUsername);
  String getDeletionUsername();
  HasModificationUser setDeletionUsername(String deletionUsername);
}
