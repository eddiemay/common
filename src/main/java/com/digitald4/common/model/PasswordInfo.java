package com.digitald4.common.model;

public class PasswordInfo {
    private String digest;
    private String resetToken;
    private long resetSentAt;

    public String getDigest() {
        return digest;
    }

    public PasswordInfo setDigest(String digest) {
        this.digest = digest;
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
