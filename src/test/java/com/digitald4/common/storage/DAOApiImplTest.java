package com.digitald4.common.storage;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

import com.digitald4.common.model.BasicUser;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.server.APIConnector;
import com.google.common.collect.ImmutableList;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DAOApiImplTest {
  private static final String BASE_API_URL = "http://test.server.net/api/%s/v1";
  private static final Long USER_ID = 123L;
  private static final String USERNAME = "username";
  private static final int TYPE_ID = 10;
  private static final String EMAIL = "user@company.com";
  private static final String FIRST_NAME = "Ricky";
  private static final String LAST_NAME = "Bobby";
  private static final BasicUser BASIC_USER = new BasicUser().setId(USER_ID).setUsername(USERNAME)
      .setTypeId(TYPE_ID).setEmail(EMAIL).setFirstName(FIRST_NAME).setLastName(LAST_NAME);
  private static final String BASIC_USER_JSON =
      String.format("{\"typeId\":%d,\"id\":%d,\"username\":\"%s\"}", TYPE_ID, USER_ID, USERNAME);
  @Mock final APIConnector connector = mock(APIConnector.class);

  private DAOApiImpl dao;

  @Before
  public void setUp() {
    dao = new DAOApiImpl(connector);

    when(connector.formatUrl(anyString()))
        .thenAnswer(i -> String.format(BASE_API_URL, i.<String>getArgument(0)));
    when(connector.sendGet(anyString())).thenReturn(BASIC_USER_JSON);
  }

  @Test
  public void create() {
    when(connector.sendPost(anyString(), anyString())).thenAnswer(
        i -> new JSONObject(i.<String>getArgument(1)).put("id", USER_ID).toString());

    BasicUser user = dao.create(new BasicUser().setUsername("user@name").setTypeId(10));

    assertThat(user.getId()).isEqualTo(USER_ID);
    assertThat(user.getUsername()).isEqualTo("user@name");
    assertThat(user.getTypeId()).isEqualTo(10);

    verify(connector).sendPost(
        "http://test.server.net/api/basicUsers/v1/create",
        "{\"typeId\":10,\"id\":0,\"username\":\"user@name\"}");
  }

  @Test
  public void get() {
    BasicUser user = dao.get(BasicUser.class, USER_ID);

    assertThat(user.getId()).isEqualTo(USER_ID);
    assertThat(user.getUsername()).isEqualTo(BASIC_USER.getUsername());
    assertThat(user.getTypeId()).isEqualTo(BASIC_USER.getTypeId());

    verify(connector).sendGet("http://test.server.net/api/basicUsers/v1/get?id=123");
  }

  @Test
  public void list_empty() {
    Query.List query = Query.forList();
    when(connector.sendGet(anyString())).thenReturn(
        new JSONObject(QueryResult.of(ImmutableList.of(BASIC_USER), 1, query)).toString());

    QueryResult<BasicUser> queryResult = dao.list(BasicUser.class, query);

    assertThat(queryResult.getTotalSize()).isEqualTo(1);
    assertThat(queryResult.getItems()).hasSize(1);

    BasicUser user = queryResult.getItems().get(0);
    assertThat(user.getId()).isEqualTo(USER_ID);
    assertThat(user.getUsername()).isEqualTo(BASIC_USER.getUsername());
    assertThat(user.getTypeId()).isEqualTo(BASIC_USER.getTypeId());

    verify(connector).sendGet("http://test.server.net/api/basicUsers/v1/list");
  }

  @Test
  public void list_withParams() {
    Query.List query = Query.forList("lastLogin>1000,typeId=10", "username", 1, 20);
    when(connector.sendGet(anyString())).thenReturn(
        new JSONObject(QueryResult.of(ImmutableList.of(BASIC_USER), 5, query)).toString());

    QueryResult<BasicUser> queryResult = dao.list(BasicUser.class, query);

    assertThat(queryResult.getTotalSize()).isEqualTo(5);
    assertThat(queryResult.getItems()).hasSize(1);

    BasicUser user = queryResult.getItems().get(0);
    assertThat(user.getId()).isEqualTo(USER_ID);
    assertThat(user.getUsername()).isEqualTo(BASIC_USER.getUsername());
    assertThat(user.getTypeId()).isEqualTo(BASIC_USER.getTypeId());

    verify(connector).sendGet("http://test.server.net/api/basicUsers/v1/list" +
        "?filter=lastLogin>1000,typeId=10&orderBy=username&pageSize=1&pageToken=20");
  }

  @Test
  public void list_limitOnly() {
    Query.List query = Query.forList().setLimit(500);
    when(connector.sendGet(anyString())).thenReturn(
        new JSONObject(QueryResult.of(ImmutableList.of(BASIC_USER), 1, query)).toString());

    QueryResult<BasicUser> queryResult = dao.list(BasicUser.class, query);

    assertThat(queryResult.getTotalSize()).isEqualTo(1);
    assertThat(queryResult.getItems()).hasSize(1);

    BasicUser user = queryResult.getItems().get(0);
    assertThat(user.getId()).isEqualTo(USER_ID);
    assertThat(user.getUsername()).isEqualTo(BASIC_USER.getUsername());
    assertThat(user.getTypeId()).isEqualTo(BASIC_USER.getTypeId());

    verify(connector).sendGet("http://test.server.net/api/basicUsers/v1/list?pageSize=500");
  }

  @Test
  public void search() {
    Query.Search query = Query.forSearch("blah");
    when(connector.sendGet(anyString())).thenReturn(
        new JSONObject(QueryResult.of(ImmutableList.of(BASIC_USER), 1, query)).toString());

    dao.search(SearchableObj.class, query);

    verify(connector).sendGet(
        "http://test.server.net/api/searchableObjs/v1/search?searchText=blah");
  }

  @Test
  public void update() {
    when(connector.send(anyString(), anyString(), anyString())).thenAnswer(i -> i.getArgument(2));

    BasicUser user = dao.update(BasicUser.class, USER_ID, current -> current.setTypeId(14));

    assertThat(user.getId()).isEqualTo(USER_ID);
    assertThat(user.getUsername()).isEqualTo(BASIC_USER.getUsername());
    assertThat(user.getTypeId()).isEqualTo(14);

    verify(connector).send(
        "PUT",
        "http://test.server.net/api/basicUsers/v1/update?id=123&updateMask=typeId",
        "{\"typeId\":14,\"id\":123,\"username\":\"username\"}");
  }

  @Test
  public void delete() {
    when(connector.send(anyString(), anyString(), anyString())).thenReturn("{}");

    dao.delete(BasicUser.class, USER_ID);

    verify(connector).send(
        "DELETE", "http://test.server.net/api/basicUsers/v1/delete?id=123", null);
  }

  @Test
  public void batchDelete() {
    when(connector.send(anyString(), anyString(), anyString())).thenReturn("3");
    dao.delete(BasicUser.class, ImmutableList.of(123L, 456L, 789L));
    verify(connector).send("POST",
        "http://test.server.net/api/basicUsers/v1/batchDelete", "{\"items\":[123,456,789]}");
  }

  public static class SearchableObj implements Searchable {}
}
