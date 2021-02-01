package com.digitald4.common.storage;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.common.model.BasicUser;
import com.digitald4.common.proto.DD4Protos;
import com.google.common.collect.ImmutableList;
import java.util.function.UnaryOperator;

import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DAOModelWrapperTest {
  private static final long USER_ID = 123L;
  private static final DD4Protos.User USER_PROTO = DD4Protos.User.newBuilder()
      .setId(USER_ID)
      .setUsername("eddiemay")
      .build();
  @Mock private final DAO<Message> messageDAO = mock(DAOCloudDS.class);
  private final DAOModelWrapper modelDAO = new DAOModelWrapper(() -> messageDAO);

  @Before
  public void setup() {
    when(messageDAO.create(any(DD4Protos.User.class))).thenAnswer(i -> i.getArguments()[0]);
    when(messageDAO.get(DD4Protos.User.class, USER_ID)).thenReturn(USER_PROTO);
    when(messageDAO.list(eq(DD4Protos.User.class), any(Query.class)))
        .thenReturn(new QueryResult<>(ImmutableList.of(USER_PROTO)));
   when(messageDAO.update(eq(DD4Protos.User.class), eq(USER_ID), any()))
       .thenAnswer(i -> i.getArgumentAt(2, UnaryOperator.class).apply(USER_PROTO));
    when(messageDAO.delete(eq(DD4Protos.User.class), any(Query.class))).thenReturn(62);
  }

  @Test
  public void testCreate() {
    BasicUser user = modelDAO.create(new BasicUser().setUsername("eddiemay"));

    assertEquals("eddiemay", user.getUsername());
  }

  @Test
  public void testGet() {
    BasicUser user = modelDAO.get(BasicUser.class, USER_ID);

    assertEquals(USER_ID, user.getId());
    assertEquals("eddiemay", user.getUsername());
  }

  @Test
  public void testList() {
    QueryResult<BasicUser> result = modelDAO.list(BasicUser.class, new Query());

    assertEquals(1, result.getTotalSize());
    BasicUser user = result.getResults().get(0);
    assertEquals(USER_ID, user.getId());
    assertEquals("eddiemay", user.getUsername());
  }

  @Test
  public void testUpdate() {
    BasicUser user = modelDAO.update(BasicUser.class, USER_ID, u -> u.setUsername("username_change"));

    assertEquals(USER_ID, user.getId());
    assertEquals(user.getUsername(), "username_change");
  }

  @Test
  public void testDelete() {
    modelDAO.delete(BasicUser.class, USER_ID);
  }

  @Test
  public void testBatchDelete() {
    int deleted = modelDAO.delete(BasicUser.class, new Query());

    assertEquals(62, deleted);
  }
}
