package com.digitald4.common.model;

import java.time.Instant;

public class Password extends ModelObject<Long> {
  private long userId;
  private String digest;
  private Instant createdAt;
  private String resetToken;
  private Instant resetSentAt;

  public Password setId(Long id) {
    super.setId(id);
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

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Password setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Password setCreatedAt(long createdAt) {
    return setCreatedAt(Instant.ofEpochMilli(createdAt));
  }

  public String getResetToken() {
    return resetToken;
  }

  public Password setResetToken(String resetToken) {
    this.resetToken = resetToken;
    return this;
  }

  public Instant getResetSentAt() {
    return resetSentAt;
  }

  public Password setResetSentAt(long resetSentAt) {
    this.resetSentAt = Instant.ofEpochMilli(resetSentAt);
    return this;
  }
}
