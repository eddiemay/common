package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.HasProto;
import com.google.protobuf.Message;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import javax.inject.Inject;

import static com.google.common.collect.ImmutableList.toImmutableList;

public class HasProtoDAO implements TypedDAO<HasProto> {
  private static final Map<Class<? extends HasProto>, Constructor<? extends HasProto>> defaultConstructors = new HashMap<>();
  private static final Map<Class<? extends HasProto>, Class<? extends Message>> protoTypeMap = new HashMap<>();
  private TypedDAO<Message> messageDAO;

  @Inject
  public HasProtoDAO(TypedDAO<Message> messageDAO) {
    this.messageDAO = messageDAO;
  }

  @Override
  public <T extends HasProto> T create(T t) {
    return fromProto((Class<T>) t.getClass(), messageDAO.create(t.toProto()));
  }

  @Override
  public <T extends HasProto> T get(Class<T> c, long id) {
    return (T) fromProto(c, messageDAO.get(getProtoType(c), id));
  }

  @Override
  public <T extends HasProto> QueryResult<T> list(Class<T> c, Query query) {
    QueryResult<? extends Message> result = messageDAO.list(getProtoType(c), query);
    return new QueryResult<T>(result.getResults().stream().map(p -> (T) fromProto(c, p)).collect(toImmutableList()));
  }

  @Override
  public <T extends HasProto> T update(Class<T> c, long id, UnaryOperator<T> updater) {
    return (T) fromProto(
        c, messageDAO.update(getProtoType(c), id, proto -> updater.apply(fromProto(c, proto)).toProto()));
  }

  @Override
  public <T extends HasProto> void delete(Class<T> c, long id) {
    messageDAO.delete(getProtoType(c), id);
  }

  @Override
  public <T extends HasProto> int delete(Class<T> c, Query query) {
    return messageDAO.delete(getProtoType(c), query);
  }

  public static <T extends HasProto> T fromProto(Class<T> c, Message m) {
    return (T) newInstance(c).fromProto(m);
  }

  public static <T extends HasProto> T newInstance(Class<T> c) {
    try {
      return (T) defaultConstructors.computeIfAbsent(c, cls -> {
        try {
          return cls.getConstructor();
        } catch (NoSuchMethodException | SecurityException e) {
          throw new DD4StorageException("Error getting default instance for type: " + c, e);
        }
      }).newInstance();
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new DD4StorageException("Error getting default instance for type: " + c, e);
    }
  }

  public static <P extends Message, T extends HasProto<P>> Class<P> getProtoType(Class<T> c) {
    return (Class<P>) protoTypeMap.computeIfAbsent(c, cls -> {
      try {
        return (Class<P>) cls.getMethod("toProto").getReturnType();
      } catch (NoSuchMethodException | SecurityException e) {
        throw new DD4StorageException("Error getting default instance for type: " + c, e);
      }
    });
  }
}
