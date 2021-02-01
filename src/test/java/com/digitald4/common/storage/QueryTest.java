package com.digitald4.common.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class QueryTest {
  @Test
  public void forValues_nullsOkay() {
    Query query = Query.forValues(null, null, 0, 0);
    assertTrue(query.getFilters().isEmpty());
    assertTrue(query.getOrderBys().isEmpty());
    assertEquals(0, query.getLimit());
    assertEquals(0, query.getOffset());
  }

  @Test
  public void forValues_parsesCorreclty() {
    Query query = Query.forValues("city=LA,championships>3", "championships DESC", 5, 1);

    assertEquals(2, query.getFilters().size());
    Query.Filter filter = query.getFilters().get(0);
    assertEquals("city", filter.getColumn());
    assertEquals("=", filter.getOperator());
    assertEquals("LA", filter.getValue());
    // filter=date=2021-01-18
    filter = query.getFilters().get(1);
    assertEquals("championships", filter.getColumn());
    assertEquals(">", filter.getOperator());
    assertEquals("3", filter.getValue());

    assertEquals(1, query.getOrderBys().size());
    Query.OrderBy orderBy =  query.getOrderBys().get(0);
    assertEquals("championships", orderBy.getColumn());
    assertTrue(orderBy.getDesc());

    assertEquals(5, query.getLimit());
    assertEquals(1, query.getOffset());
  }

  @Test
  public void forValues_canparseADate() {
    Query query = Query.forValues("date=2021-01-18", "", 92, 12);

    assertEquals(1, query.getFilters().size());
    Query.Filter filter = query.getFilters().get(0);
    assertEquals("date", filter.getColumn());
    assertEquals("=", filter.getOperator());
    assertEquals("2021-01-18", filter.getValue());

    assertTrue(query.getOrderBys().isEmpty());

    assertEquals(92, query.getLimit());
    assertEquals(12, query.getOffset());
  }
}