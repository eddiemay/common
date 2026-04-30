package com.digitald4.common.model;

import java.time.Instant;

public class Password extends ModelObjectModUser<Long> {
  private String username;
  private String digest;
  private String resetToken;
  private Instant resetSentAt;

  public Password setId(Long id) {
    super.setId(id);
    return this;
  }

  public String getUsername() {
    return username;
  }

  public Password setUsername(String username) {
    this.username = username;
    return this;
  }

  public String getDigest() {
    return digest;
  }

  public Password setDigest(String digest) {
    this.digest = digest;
    return this;
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
