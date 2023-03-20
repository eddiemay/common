package com.digitald4.common.storage;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class QueryTest {
  @Test
  public void forValues_nullsOkay() {
    Query.List query = Query.forList(null, null, null, 0);

    assertThat(query.getFilters()).isEmpty();
    assertThat(query.getOrderBys()).isEmpty();
    assertThat(query.getLimit()).isNull();
    assertThat(query.getOffset()).isEqualTo(0);
  }

  @Test
  public void forValues_parsesCorrectly() {
    Query.List query = Query.forList("city=LA,championships>3", "championships DESC", 5, 2);

    assertThat(query.getFilters()).hasSize(2);
    Query.Filter filter = query.getFilters().get(0);
    assertThat(filter.getColumn()).isEqualTo("city");
    assertThat(filter.getOperator()).isEqualTo("=");
    assertThat(filter.getValue()).isEqualTo("LA");
    // filter=date=2021-01-18
    filter = query.getFilters().get(1);
    assertThat(filter.getColumn()).isEqualTo("championships");
    assertThat(filter.getOperator()).isEqualTo(">");
    assertThat(filter.getValue()).isEqualTo("3");

    assertThat(query.getOrderBys()).hasSize(1);
    Query.OrderBy orderBy =  query.getOrderBys().get(0);
    assertThat(orderBy.getColumn()).isEqualTo("championships");
    assertThat(orderBy.getDesc()).isTrue();

    assertThat(query.getLimit()).isEqualTo(5);
    assertThat(query.getOffset()).isEqualTo(5);
  }

  @Test
  public void forValues_canParseADate() {
    Query.List query = Query.forList("date=2021-01-18", "", 92, 12);

    assertThat(query.getFilters()).hasSize(1);
    Query.Filter filter = query.getFilters().get(0);
    assertThat(filter.getColumn()).isEqualTo("date");
    assertThat(filter.getOperator()).isEqualTo("=");
    assertThat(filter.getValue()).isEqualTo("2021-01-18");

    assertThat(query.getOrderBys()).isEmpty();

    assertThat(query.getPageSize()).isEqualTo(92);
    assertThat(query.getLimit()).isEqualTo(92);
    assertThat(query.getPageToken()).isEqualTo(12);
    assertThat(query.getOffset()).isEqualTo(11 * 92);
  }

  @Test
  public void forValues_canParseIn() {
    Query.List query = Query.forList("type IN 1|2|3", null, null, 0);

    assertThat(query.getFilters()).hasSize(1);
    Query.Filter filter = query.getFilters().get(0);
    assertThat(filter.getColumn()).isEqualTo("type");
    assertThat(filter.getOperator()).isEqualTo("IN");
    assertThat((Iterable<?>) filter.getValue()).containsExactly("1", "2", "3");
  }

  @Test
  public void forValues_canParseEnumValue() {
    Query.List query = Query.forList("status=Unconfirmed", null, null, 0);

    assertThat(query.getFilters()).hasSize(1);
    Query.Filter filter = query.getFilters().get(0);
    assertThat(filter.getColumn()).isEqualTo("status");
    assertThat(filter.getOperator()).isEqualTo("=");
    assertThat(filter.getValue()).isEqualTo("Unconfirmed");
  }

  @Test
  public void forValues_canParseStringValue() {
    Query.List query = Query.forList("entityType = Nurse", null, null, 0);

    assertThat(query.getFilters()).hasSize(1);
    Query.Filter filter = query.getFilters().get(0);
    assertThat(filter.getColumn()).isEqualTo("entityType");
    assertThat(filter.getOperator()).isEqualTo("=");
    assertThat(filter.getValue()).isEqualTo("Nurse");
  }
}