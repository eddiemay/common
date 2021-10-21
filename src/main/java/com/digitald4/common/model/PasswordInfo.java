package com.digitald4.common.model;

public class PasswordInfo {
  private long id;
  private long userId;
  private String digest;
  private long lastUpdated;
  private String resetToken;
  private long resetSentAt;

  public long getId() {
    return id;
  }

  public PasswordInfo setId(long id) {
    this.id = id;
    return this;
  }

  public long getUserId() {
    return userId;
  }

  public PasswordInfo setUserId(long userId) {
    this.userId = userId;
    return this;
  }

  public String getDigest() {
    return digest;
  }

  public PasswordInfo setDigest(String digest) {
    this.digest = digest;
    return this;
  }

  public long getLastUpdated() {
    return lastUpdated;
  }

  public PasswordInfo setLastUpdated(long lastUpdated) {
    this.lastUpdated = lastUpdated;
    return this;
  }

  public String getResetToken() {
    return resetToken;
  }

  public PasswordInfo setResetToken(String resetToken) {
    this.resetToken = resetToken;
    return this;
  }

  public long getResetSentAt() {
    return resetSentAt;
  }

  public PasswordInfo setResetSentAt(long resetSentAt) {
    this.resetSentAt = resetSentAt;
    return this;
  }
}
