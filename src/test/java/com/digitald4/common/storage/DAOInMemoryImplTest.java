package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Transaction.Op;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class DAOInMemoryImplTest {
  private DAOInMemoryImpl dao;

  private static final Person ADAM = new Person().setName("Adam").setAge(930);
  private static final Person SETH = new Person().setName("Seth").setAge(900);
  private static final Person ENOCH = new Person().setName("Enoch").setAge(365);
  private static final Person MOSES = new Person().setName("Moses").setAge(120);

  @Before
  public void setup() {
    dao = new DAOInMemoryImpl(new ChangeTracker( null, null, null, null));
    dao.persist(Transaction.of(Stream.of(ADAM, SETH, ENOCH, MOSES).map(Op::create).collect(toImmutableList())));
  }

  @Test
  public void exactMatch() {
    assertThat(dao
        .list(Person.class, Query.forList().setFilters(Filter.of("name", "=", "Adam")))
        .getItems()).containsExactly(ADAM);
    assertThat(dao
        .list(Person.class, Query.forList().setFilters(Filter.of("age", "=", 930)))
        .getItems()).containsExactly(ADAM);
  }

  @Test
  public void greaterThan() {
    assertThat(dao
        .list(Person.class, Query.forList().setFilters(Filter.of("age", ">", 365)))
        .getItems()).containsExactly(ADAM, SETH);
  }

  @Test
  public void lessThan() {
    assertThat(dao
        .list(Person.class, Query.forList().setFilters(Filter.of("age", "<", 900)))
        .getItems()).containsExactly(ENOCH, MOSES);
  }

  @Test
  public void withInRange() {
    assertThat(dao
        .list(Person.class, Query.forList().setFilters(Filter.of("age", ">", 120), Filter.of("age", "<", 900)))
        .getItems()).containsExactly(ENOCH);
  }

  public static class Person {
    private Long id;
    private String name;
    private int age;

    public Long getId() {
      return id;
    }

    public Person setId(Long id) {
      this.id = id;
      return this;
    }

    public String getName() {
      return name;
    }

    public Person setName(String name) {
      this.name = name;
      return this;
    }

    public int getAge() {
      return age;
    }

    public Person setAge(int age) {
      this.age = age;
      return this;
    }

    @Override
    public String toString() {
      return getName() + " " + getAge();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Person && toString().equals(obj.toString());
    }
  }
}
