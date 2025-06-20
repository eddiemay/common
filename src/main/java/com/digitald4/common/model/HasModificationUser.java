package com.digitald4.common.model;

public interface HasModificationUser<ID> extends HasModificationTimes<ID> {
  @Deprecated Long getCreationUserId();
  @Deprecated HasModificationUser<ID> setCreationUserId(Long userId);
  @Deprecated Long getLastModifiedUserId();
  @Deprecated HasModificationUser<ID> setLastModifiedUserId(Long userId);

  String getCreationUsername();
  HasModificationUser<ID> setCreationUsername(String creationUsername);
  String getLastModifiedUsername();
  HasModificationUser<ID> setLastModifiedUsername(String lastModifiedUsername);
}
