package com.digitald4.common.model;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import org.joda.time.DateTime;

public class Session {
  private String id;
  private long userId;
  private String username;
  private DateTime startTime;
  private DateTime expTime;
  private DateTime endTime;

  public enum State {ACTIVE, CLOSED}
  private State state;

  private User user;

  public String getId() {
    return id;
  }

  public Session setId(String id) {
    this.id = id;
    return this;
  }

  public long getUserId() {
    return userId;
  }

  public Session setUserId(long userId) {
    this.userId = userId;
    return this;
  }

  public String getUsername() {
    return username;
  }

  public Session setUsername(String username) {
    this.username = username;
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

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  public DateTime getExpTime() {
    return expTime;
  }

  public Session setExpTime(DateTime expTime) {
    this.expTime = expTime;
    return this;
  }

  @ApiResourceProperty
  public long expTime() {
    return expTime == null ? 0 : expTime.getMillis();
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  public DateTime getEndTime() {
    return endTime;
  }

  public Session setEndTime(DateTime endTime) {
    this.endTime = endTime;
    return this;
  }

  @ApiResourceProperty
  public Long endTime() {
    return endTime == null ? null : endTime.getMillis();
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
