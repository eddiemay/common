package com.digitald4.common.model;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import org.joda.time.DateTime;

public class Session {
  private long id;
  private String idToken;
  private long userId;
  private DateTime startTime;
  private long expTime;
  private long endTime;

  public enum State {ACTIVE, CLOSED}
  private State state;

  private User user;

  public long getId() {
    return id;
  }

  public Session setId(long id) {
    this.id = id;
    return this;
  }

  public String getIdToken() {
    return idToken;
  }

  public Session setIdToken(String idToken) {
    this.idToken = idToken;
    return this;
  }

  public long getUserId() {
    return userId;
  }

  public Session setUserId(long userId) {
    this.userId = userId;
    return this;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  public DateTime getStartTime() {
    return startTime;
  }

  public Session setStartTime(DateTime startTime) {
    this.startTime = startTime;
    return this;
  }

  @ApiResourceProperty
  public long startTime() {
    return startTime == null ? 0 : startTime.getMillis();
  }

  public long getExpTime() {
    return expTime;
  }

  public Session setExpTime(long expTime) {
    this.expTime = expTime;
    return this;
  }

  public long getEndTime() {
    return endTime;
  }

  public Session setEndTime(long endTime) {
    this.endTime = endTime;
    return this;
  }

  public State getState() {
    return state;
  }

  public Session setState(State state) {
    this.state = state;
    return this;
  }

  @ApiResourceProperty
  public <U extends User> U user() {
    return (U) user;
  }

  public <U extends User> Session user(U user) {
    this.user = user;
    return this;
  }
}
