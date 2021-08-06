package com.digitald4.common.model;

public class ActiveSession {
    private long id;
    private String idToken;
    private long expTime;
    private long userId;

    public long getId() {
        return id;
    }

    public ActiveSession setId(long id) {
        this.id = id;
        return this;
    }

    public String getIdToken() {
        return idToken;
    }

    public ActiveSession setIdToken(String idToken) {
        this.idToken = idToken;
        return this;
    }

    public long getExpTime() {
        return expTime;
    }

    public ActiveSession setExpTime(long expTime) {
        this.expTime = expTime;
        return this;
    }

    public long getUserId() {
        return userId;
    }

    public ActiveSession setUserId(long userId) {
        this.userId = userId;
        return this;
    }
}
