package com.digitald4.common.model;

import org.joda.time.DateTime;

public class Password {
  private long id;
  private long userId;
  private String digest;
  private DateTime createdAt;
  private String resetToken;
  private DateTime resetSentAt;

  public long getId() {
    return id;
  }

  public Password setId(long id) {
    this.id = id;
    return this;
  }

  public long getUserId() {
    return userId;
  }

  public Password setUserId(long userId) {
    this.userId = userId;
    return this;
  }

  public String getDigest() {
    return digest;
  }

  public Password setDigest(String digest) {
    this.digest = digest;
    return this;
  }

  public DateTime getCreatedAt() {
    return createdAt;
  }

  public Password setCreatedAt(DateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public String getResetToken() {
    return resetToken;
  }

  public Password setResetToken(String resetToken) {
    this.resetToken = resetToken;
    return this;
  }

  public DateTime getResetSentAt() {
    return resetSentAt;
  }

  public Password setResetSentAt(DateTime resetSentAt) {
    this.resetSentAt = resetSentAt;
    return this;
  }
}
