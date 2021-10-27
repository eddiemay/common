package com.digitald4.common.storage;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class Annotations {
  private Annotations() {}

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  public @interface DefaultDAO {}

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  public @interface SessionCacheEnabled {}

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  public @interface SessionDuration {}
}
