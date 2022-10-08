package com.digitald4.common.storage;

import java.time.Clock;

public interface DAO extends TypedDAO<Object> {
  Clock getClock();
}
