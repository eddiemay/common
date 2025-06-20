package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.google.common.collect.ImmutableList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Query {
  private static final Pattern FILTER_PATTERN =
      Pattern.compile("([A-Za-z_]+)\\s*([!=<>]+|IN)\\s*([A-Za-z0-9-_|\\. ]+)");

  private ImmutableList<String> fields = ImmutableList.of();
  private ImmutableList<OrderBy> orderBys = ImmutableList.of();
  private Integer pageSize;
  private int pageToken;
  private boolean useDBSort = false;

  private Query() {}

  public Query setFields(Iterable<String> fields) {
    this.fields = ImmutableList.copyOf(fields);
    return this;
  }

  public ImmutableList<String> getFields() {
    return fields;
  }

  public ImmutableList<OrderBy> getOrderBys() {
    return orderBys;
  }

  public Query setOrderBys(Iterable<OrderBy> orderBys) {
    this.orderBys = ImmutableList.copyOf(orderBys);
    return this;
  }

  public Query setOrderBys(OrderBy... orderBys) {
    return setOrderBys(asList(orderBys));
  }

  public boolean useDBSort() {
    return useDBSort;
  }

  public Query setUseDBSort(boolean useDBSort) {
    this.useDBSort = useDBSort;
    return this;
  }

  public Query setPageSize(Integer pageSize) {
    if (pageSize != null && pageSize < 0) {
      throw new DD4StorageException("Pagesize must be zero or greater", ErrorCode.BAD_REQUEST);
    }
    this.pageSize = pageSize;
    return this;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public Query setLimit(Integer limit) {
    return setPageSize(pageSize);
  }

  public Integer getLimit() {
    return getPageSize();
  }

  public Query setPageToken(int pageToken) {
    this.pageToken = Math.max(pageToken, 1);
    return this;
  }

  public int getPageToken() {
    return pageToken;
  }

  public int getOffset() {
    Integer pageSize = getPageSize();
    return (pageSize == null ? 0 : pageSize) * (getPageToken() - 1);
  }

  public static Query.List forList() {
    return new Query.List();
  }

  public static List forList(Filter... filters) {
    return new List().setFilters(filters);
  }

  public static Query.List forList(String fields, String filters, String orderBys, Integer pageSize, int pageToken) {
    Query.List query = new Query.List();
    if (fields != null && !fields.isEmpty()) {
      query.setFields(stream(fields.split(",")).collect(toImmutableList()));
    }
    if (filters != null && !filters.isEmpty()) {
      query.setFilters(stream(filters.split(",")).map(Filter::parse).collect(toImmutableList()));
    }
    if (orderBys != null && !orderBys.isEmpty()) {
      query.setOrderBys(stream(orderBys.split(","))
          .map(orderBy -> OrderBy.of(orderBy.split(" ")[0], orderBy.endsWith("DESC")))
          .collect(toImmutableList()));
    }

    query.setPageSize(pageSize).setPageToken(pageToken);
    return query;

  }

  public static Query.Search forSearch(String searchText) {
    return new Query.Search(searchText);
  }

  public static Query.Search forSearch(String searchText, String orderBys, int pageSize, int pageToken) {
    Query.Search query = new Query.Search(searchText);
    query.setPageSize(pageSize).setPageToken(pageToken);
    if (orderBys != null && !orderBys.isEmpty()) {
      query.setOrderBys(stream(orderBys.split(","))
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
      return setFilters(asList(filters));
    }

    public Query.List addFilter(Filter filter) {
      return setFilters(ImmutableList.<Filter>builder().addAll(filters).add(filter).build());
    }

    @Override
    public Query.List setOrderBys(OrderBy... orderBys) {
      super.setOrderBys(orderBys);
      return this;
    }

    @Override
    public Query.List setLimit(Integer limit) {
      super.setPageSize(limit);
      return this;
    }

    public Query.List setOffset(int offset) {
      super.setPageToken(offset);
      return this;
    }

    public String toString() {
      return filters.stream().map(Filter::toString).collect(Collectors.joining(","));
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

    @Override
    public Search setPageSize(Integer pageSize) {
      super.setPageSize(pageSize);
      return this;
    }
  }

  public static class Filter {
    private final String column;
    private final String operator;
    private final Object value;

    private Filter(String column, String operator, Object value) {
      this.column = column;
      this.operator = operator;
      this.value = "null".equalsIgnoreCase(String.valueOf(value)) ? null : value;
    }

    public static Filter of(String column, String operator, Object value) {
      return new Filter(column, operator, value);
    }

    public static Filter of(String column, Object value) {
      return new Filter(column, "=", value);
    }

    public static Filter parse(String filter) {
      Matcher matcher = FILTER_PATTERN.matcher(filter);
      if (!matcher.find()) {
        return null;
      }

      String operator = matcher.group(2).trim();
      Object value = matcher.group(3).trim();
      if (operator.equals("IN")) {
        value = asList(value.toString().split("\\|"));
      }

      return new Filter(matcher.group(1).trim(), operator, value);
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

    public String toString() {
      return String.format("%s %s %s", column, operator == null ? "=" : operator, value);
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
