package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import javax.inject.Inject;
import javax.inject.Provider;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class SequenceStore {
  private final Provider<DAO> daoProvider;

  @Inject
  public SequenceStore(Provider<DAO> daoProvider) {
    this.daoProvider = daoProvider;
  }

  public synchronized long getAndIncrement(Class<?> cls) {
    Sequence sequence = daoProvider.get().get(Sequence.class, cls.getSimpleName());
    long value = sequence.getValue();
    daoProvider.get().create(sequence.increment());
    return value;
  }

  public synchronized ImmutableList<Long> allocate(Class<?> cls, long number) {
    Sequence sequence = daoProvider.get().get(Sequence.class, cls.getSimpleName());
    long start = sequence.getValue();
    daoProvider.get().create(sequence.setValue(start + number));
    return LongStream.range(start, start + number).boxed().collect(toImmutableList());
  }

  public void create(Class<?> cls, long initialValue) {
    daoProvider.get().create(new Sequence().setId(cls.getSimpleName()).setValue(initialValue));
  }

  public static class Sequence {
    private String id;
    private long value = 1;

    public String getId() {
      return id;
    }

    public Sequence setId(String id) {
      this.id = id;
      return this;
    }

    public long getValue() {
      return value;
    }

    public Sequence setValue(long value) {
      this.value = value;
      return this;
    }

    public Sequence increment() {
      value++;
      return this;
    }
  }
}
