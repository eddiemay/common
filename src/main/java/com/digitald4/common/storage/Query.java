package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Query {
  private static final Pattern FILTER_PATTERN = Pattern.compile("([A-Za-z_ ]+)([!=<>]+)([A-Za-z0-9-_ ]+)");

  private ImmutableList<Filter> filters = ImmutableList.of();
  private ImmutableList<OrderBy> orderBys = ImmutableList.of();
  private int offset;
  private int limit;

  public ImmutableList<Filter> getFilters() {
    return filters;
  }

  public Query setFilters(Iterable<Filter> filters) {
    this.filters = ImmutableList.copyOf(filters);
    return this;
  }

  public Query setFilters(Filter... filters) {
    return setFilters(Arrays.asList(filters));
  }

  public ImmutableList<OrderBy> getOrderBys() {
    return orderBys;
  }

  public Query setOrderBys(Iterable<OrderBy> orderBys) {
    this.orderBys = ImmutableList.copyOf(orderBys);
    return this;
  }

  public Query setOrderBys(OrderBy... orderBys) {
    return setOrderBys(Arrays.asList(orderBys));
  }

  public int getOffset() {
    return offset;
  }

  public Query setOffset(int offset) {
    this.offset = offset;
    return this;
  }

  public int getLimit() {
    return limit;
  }

  public Query setLimit(int limit) {
    this.limit = limit;
    return this;
  }

  public static Query forValues(String filters, String orderBys, int limit, int offset) {
    Query query = new Query()
        .setLimit(limit)
        .setOffset(offset);
    if (filters != null && !filters.isEmpty()) {
      query.setFilters(Arrays.stream(filters.split(","))
          .map(filter -> {
            Matcher matcher = FILTER_PATTERN.matcher(filter);
            if (matcher.find()) {
              return new Query.Filter()
                  .setColumn(matcher.group(1).trim())
                  .setOperator(matcher.group(2))
                  .setValue(matcher.group(3).trim());
            }
            return null;
          })
          .collect(toImmutableList()));
    }
    if (orderBys != null && !orderBys.isEmpty()) {
      query.setOrderBys(Arrays.stream(orderBys.split(","))
          .map(orderBy -> new Query.OrderBy()
              .setColumn(orderBy.split(" ")[0])
              .setDesc(orderBy.endsWith("DESC")))
          .collect(toImmutableList()));
    }

    return query;
  }

  public static class Filter {
    private String column;
    private String operator = "=";
    private Object value;

    public String getColumn() {
      return column;
    }

    public Filter setColumn(String column) {
      this.column = column;
      return this;
    }

    public String getOperator() {
      return operator;
    }

    public Filter setOperator(String operator) {
      this.operator = operator;
      return this;
    }

    public Object getValue() {
      return value;
    }

    public Filter setValue(Object value) {
      this.value = value;
      return this;
    }

    public <T> T getVal() {
      return (T) value;
    }
  }

  public static class OrderBy {
    private String column;
    private boolean desc;

    public String getColumn() {
      return column;
    }

    public OrderBy setColumn(String column) {
      this.column = column;
      return this;
    }

    public boolean getDesc() {
      return desc;
    }

    public OrderBy setDesc(boolean desc) {
      this.desc = desc;
      return this;
    }
  }
}
