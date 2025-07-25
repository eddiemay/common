package com.digitald4.common.model;

public class ModelObjectModUser<ID> extends ModelObjectModTime<ID> implements HasModificationUser<ID> {
  private String creationUsername;
  private String lastModifiedUsername;
  private String deletionUsername;
  private Long creationUserId;
  private Long lastModifiedUserId;

  @Override
  public String getCreationUsername() {
    return creationUsername;
  }

  @Override
  public HasModificationUser<ID> setCreationUsername(String creationUsername) {
    this.creationUsername = creationUsername;
    return this;
  }

  @Override
  public String getLastModifiedUsername() {
    return lastModifiedUsername;
  }

  @Override
  public HasModificationUser<ID> setLastModifiedUsername(String lastModifiedUsername) {
    this.lastModifiedUsername = lastModifiedUsername;
    return this;
  }

  public String getDeletionUsername() {
    return deletionUsername;
  }

  public HasModificationUser<ID> setDeletionUsername(String deletionUsername) {
    this.deletionUsername = deletionUsername;
    return this;
  }

  @Override
  public Long getCreationUserId() {
    return creationUserId;
  }

  public ModelObjectModUser<ID> setCreationUserId(Long creationUserId) {
    this.creationUserId = creationUserId;
    return this;
  }

  @Override
  public Long getLastModifiedUserId() {
    return lastModifiedUserId;
  }

  public ModelObjectModUser<ID> setLastModifiedUserId(Long lastModifiedUserId) {
    this.lastModifiedUserId = lastModifiedUserId;
    return this;
  }
}
