package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.common.exception.DD4StorageException;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Query {
  private static final Pattern FILTER_PATTERN = Pattern.compile("([A-Za-z_ ]+)([!=<>]+)([A-Za-z0-9-_ ]+)");

  private ImmutableList<OrderBy> orderBys = ImmutableList.of();
  private int pageSize;
  private int pageToken;

  private Query() {}

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

  public Query setPageSize(int pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  public int getPageSize() {
    return pageSize;
  }

  public Query setLimit(int limit) {
    return setPageSize(pageSize);
  }

  public int getLimit() {
    return getPageSize();
  }

  public Query setPageToken(int pageToken) {
    if (pageToken < 1) {
      pageToken = 1;
      // throw new DD4StorageException("Pagetoken must be greater than zero", DD4StorageException.ErrorCode.BAD_REQUEST);
    }
    this.pageToken = pageToken;
    return this;
  }

  public int getPageToken() {
    return pageToken;
  }

  public int getOffset() {
    return getPageSize() * (getPageToken() - 1);
  }

  public static Query.List forList() {
    return new Query.List();
  }

  public static Query.List forList(String filters, String orderBys, int pageSize, int pageToken) {
    Query.List query = new Query.List();
    query.setPageSize(pageSize).setPageToken(pageToken);
    if (filters != null && !filters.isEmpty()) {
      query.setFilters(Arrays.stream(filters.split(","))
          .map(filter -> {
            Matcher matcher = FILTER_PATTERN.matcher(filter);
            if (matcher.find()) {
              return Filter.of(matcher.group(1).trim(), matcher.group(2), matcher.group(3).trim());
            }
            return null;
          })
          .collect(toImmutableList()));
    }
    if (orderBys != null && !orderBys.isEmpty()) {
      query.setOrderBys(Arrays.stream(orderBys.split(","))
          .map(orderBy -> OrderBy.of(orderBy.split(" ")[0], orderBy.endsWith("DESC")))
          .collect(toImmutableList()));
    }

    return query;
  }

  public static Query.Search forSearch(String searchText) {
    return new Query.Search(searchText);
  }

  public static Query.Search forSearch(String searchText, String orderBys, int pageSize, int pageToken) {
    Query.Search query = new Query.Search(searchText);
    query.setPageSize(pageSize).setPageToken(pageToken);
    if (orderBys != null && !orderBys.isEmpty()) {
      query.setOrderBys(Arrays.stream(orderBys.split(","))
          .map(orderBy -> OrderBy.of(orderBy.split(" ")[0], orderBy.endsWith("DESC")))
          .collect(toImmutableList()));
    }

    return query;
  }

  public static class List extends Query {
    private ImmutableList<Filter> filters = ImmutableList.of();

    private List(){}

    public ImmutableList<Filter> getFilters() {
      return filters;
    }

    public Query.List setFilters(Iterable<Filter> filters) {
      this.filters = ImmutableList.copyOf(filters);
      return this;
    }

    public Query.List setFilters(Filter... filters) {
      return setFilters(Arrays.asList(filters));
    }

    @Override
    public Query.List setOrderBys(OrderBy... orderBys) {
      super.setOrderBys(orderBys);
      return this;
    }

    public Query.List setLimit(int limit) {
      super.setPageToken(limit);
      return this;
    }

    public Query.List setOffset(int offset) {
      super.setPageToken(offset);
      return this;
    }
  }

  public static class Search extends Query {
    private final String searchText;

    private Search(String searchText) {
      this.searchText = searchText;
    }

    public String getSearchText() {
      return searchText;
    }
  }

  public static class Filter {
    private final String column;
    private final String operator;
    private final Object value;

    private Filter(String column, String operator, Object value) {
      this.column = column;
      this.operator = operator;
      this.value = value;
    }

    public static Filter of(String column, String operator, Object value) {
      return new Filter(column, operator, value);
    }

    public static Filter of(String column, Object value) {
      return new Filter(column, "=", value);
    }

    public String getColumn() {
      return column;
    }

    public String getOperator() {
      return operator;
    }

    public Object getValue() {
      return value;
    }

    public <T> T getVal() {
      return (T) value;
    }
  }

  public static class OrderBy {
    private final String column;
    private final boolean desc;

    private OrderBy(String column, boolean desc) {
      this.column = column;
      this.desc = desc;
    }

    public static OrderBy of(String column) {
      return new OrderBy(column, false);
    }

    public static OrderBy of(String column, boolean desc) {
      return new OrderBy(column, desc);
    }

    public String getColumn() {
      return column;
    }

    public boolean getDesc() {
      return desc;
    }
  }
}
