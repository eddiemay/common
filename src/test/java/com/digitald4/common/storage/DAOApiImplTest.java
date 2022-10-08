package com.digitald4.common.storage;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.*;

import com.digitald4.common.model.BasicUser;
import com.digitald4.common.server.APIConnector;
import com.google.common.collect.ImmutableList;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DAOApiImplTest {
  private static final Long USER_ID = 123L;
  private static final String BASE_API_URL = "http://test.server.net/api/%s/v1";
  private static final BasicUser BASIC_USER = new BasicUser().setId(USER_ID).setUsername("user@name").setTypeId(10);
  private static final String BASIC_USER_JSON = "{\"typeId\":10,\"id\":" + USER_ID + ",\"username\":\"user@name\"}";
  @Mock final APIConnector connector = mock(APIConnector.class);

  private DAOApiImpl dao;

  @Before
  public void setUp() {
    dao = new DAOApiImpl(connector, null);

    when(connector.formatUrl(anyString()))
        .thenAnswer(i -> String.format(BASE_API_URL, i.getArgumentAt(0, String.class)));
    when(connector.sendGet(anyString())).thenReturn(BASIC_USER_JSON);
  }

  @Test
  public void create() {
    when(connector.sendPost(anyString(), anyString())).thenAnswer(
        i -> new JSONObject(i.getArgumentAt(1, String.class)).put("id", USER_ID).toString());

    BasicUser user = dao.create(new BasicUser().setUsername("user@name").setTypeId(10));

    assertEquals(USER_ID, user.getId());
    assertEquals("user@name", user.getUsername());
    assertEquals(10, user.getTypeId());

    verify(connector).sendPost(
        "http://test.server.net/api/basicUsers/v1/_",
        "{\"typeId\":10,\"id\":0,\"username\":\"user@name\"}");
  }

  @Test
  public void get() {
    BasicUser user = dao.get(BasicUser.class, USER_ID);

    assertEquals(USER_ID, user.getId());
    assertEquals(BASIC_USER.getUsername(), user.getUsername());
    assertEquals(BASIC_USER.getTypeId(), user.getTypeId());

    verify(connector).sendGet("http://test.server.net/api/basicUsers/v1/123");
  }

  @Test
  public void list() {
    Query.List query = Query.forList("lastLogin>1000,typeId=10", "username", 1, 20);
    when(connector.sendGet(anyString()))
        .thenReturn(new JSONObject(QueryResult.of(ImmutableList.of(BASIC_USER), 5, query)).toString());

    QueryResult<BasicUser> queryResult = dao.list(BasicUser.class, query);

    assertEquals(5, queryResult.getTotalSize());
    assertEquals(1, queryResult.getItems().size());

    BasicUser user = queryResult.getItems().get(0);
    assertEquals(USER_ID, user.getId());
    assertEquals(BASIC_USER.getUsername(), user.getUsername());
    assertEquals(BASIC_USER.getTypeId(), user.getTypeId());

    verify(connector).sendGet(
        "http://test.server.net/api/basicUsers/v1/_?filter=lastLogin>1000,typeId=10&orderBy=username&pageSize=1&pageToken=20");
  }

  @Test
  public void update() {
    when(connector.send(anyString(), anyString(), anyString())).thenAnswer(i -> i.getArgumentAt(2, String.class));

    BasicUser user = dao.update(BasicUser.class, USER_ID, current -> current.setTypeId(14));

    assertEquals(USER_ID, user.getId());
    assertEquals(BASIC_USER.getUsername(), user.getUsername());
    assertEquals(14, user.getTypeId());

    verify(connector).send(
        "PUT",
        "http://test.server.net/api/basicUsers/v1/123?updateMask=typeId",
        "{\"typeId\":14,\"id\":123,\"username\":\"user@name\"}");
  }

  @Test
  public void delete() {
    when(connector.send(anyString(), anyString(), anyString())).thenReturn("{}");

    dao.delete(BasicUser.class, USER_ID);

    verify(connector).send("DELETE", "http://test.server.net/api/basicUsers/v1/123", null);
  }

  @Test
  public void batchDelete() {
    when(connector.send(anyString(), anyString(), anyString())).thenReturn("{deleted: 3}");
    dao.delete(BasicUser.class, ImmutableList.of(123L, 456L, 789L));
    verify(connector).send(
        "POST", "http://test.server.net/api/basicUsers/v1/batchDelete", "[123,456,789]");
  }
}
