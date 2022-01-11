package com.digitald4.common.storage;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.common.model.HasProto;
import com.digitald4.common.model.User;
import com.digitald4.common.proto.DD4Protos;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import java.util.function.UnaryOperator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class HasProtoDAOTest {
  private static final long USER_ID = 123L;
  private static final DD4Protos.User USER_PROTO = DD4Protos.User.newBuilder()
      .setId(USER_ID)
      .setUsername("eddiemay")
      .build();
  @Mock private final TypedDAO<Message> messageDAO = mock(DAOCloudDSProto.class);
  private HasProtoDAO modelDAO;

  @Before
  public void setup() {
    modelDAO = new HasProtoDAO(messageDAO);
    when(messageDAO.create(any(DD4Protos.User.class))).thenAnswer(i -> i.getArguments()[0]);
    when(messageDAO.get(DD4Protos.User.class, USER_ID)).thenReturn(USER_PROTO);
    when(messageDAO.list(eq(DD4Protos.User.class), any(Query.List.class)))
        .thenReturn(QueryResult.of(ImmutableList.of(USER_PROTO), 1, null));
    when(messageDAO.update(eq(DD4Protos.User.class), eq(USER_ID), any()))
        .thenAnswer(i -> i.getArgumentAt(2, UnaryOperator.class).apply(USER_PROTO));
  }

  @Test
  public void testCreate() {
    HasProtoUser user = modelDAO.create(new HasProtoUser().setUsername("eddiemay"));

    assertEquals("eddiemay", user.getUsername());
  }

  @Test
  public void testGet() {
    HasProtoUser user = modelDAO.get(HasProtoUser.class, USER_ID);

    assertEquals(USER_ID, user.getId());
    assertEquals("eddiemay", user.getUsername());
  }

  @Test
  public void testList() {
    QueryResult<HasProtoUser> result = modelDAO.list(HasProtoUser.class, Query.forList());

    assertEquals(1, result.getTotalSize());
    HasProtoUser user = result.getResults().get(0);
    assertEquals(USER_ID, user.getId());
    assertEquals("eddiemay", user.getUsername());
  }

  @Test
  public void testUpdate() {
    HasProtoUser user = modelDAO.update(HasProtoUser.class, USER_ID, u -> u.setUsername("username_change"));

    assertEquals(USER_ID, user.getId());
    assertEquals(user.getUsername(), "username_change");
  }

  @Test
  public void testDelete() {
    modelDAO.delete(HasProtoUser.class, USER_ID);
  }

  @Test
  public void testBatchDelete() {
    modelDAO.delete(HasProtoUser.class, ImmutableList.of(123L, 456L, 789L, 101112L, 131415L));
  }

  public static class HasProtoUser implements User, HasProto<DD4Protos.User> {

    private DD4Protos.User proto;
    public HasProtoUser() {
      proto = DD4Protos.User.getDefaultInstance();
    }

    @Override
    public long getId() {
      return proto.getId();
    }

    @Override
    public HasProtoUser setId(long id) {
      proto = proto.toBuilder().setId(id).build();
      return this;
    }

    @Override
    public String getUsername() {
      return proto.getUsername();
    }

    @Override
    public HasProtoUser setUsername(String username) {
      proto = proto.toBuilder().setUsername(username).build();
      return this;
    }

    @Override
    public String getEmail() {
      return proto.getEmail();
    }

    @Override
    public HasProtoUser setEmail(String email) {
      proto = proto.toBuilder().setEmail(email).build();
      return this;
    }

    @Override
    public String getFirstName() {
      return proto.getFirstName();
    }

    @Override
    public HasProtoUser setFirstName(String firstName) {
      proto = proto.toBuilder().setFirstName(firstName).build();
      return this;
    }

    @Override
    public String getLastName() {
      return proto.getLastName();
    }

    @Override
    public HasProtoUser setLastName(String lastName) {
      proto = proto.toBuilder().setLastName(lastName).build();
      return this;
    }

    public String fullName() {
      return String.format("%s %s", getFirstName(), getLastName());
    }

    @Override
    public int getTypeId() {
      return proto.getTypeId();
    }

    @Override
    public HasProtoUser setTypeId(int typeId) {
      proto = proto.toBuilder().setTypeId(typeId).build();
      return this;
    }

    @Override
    public DD4Protos.User toProto() {
      return proto;
    }

    @Override
    public HasProto<DD4Protos.User> fromProto(DD4Protos.User proto) {
      this.proto = proto;
      return this;
    }
  }
}
