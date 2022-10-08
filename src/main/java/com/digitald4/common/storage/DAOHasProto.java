package com.digitald4.common.storage;

import static com.google.common.collect.Streams.stream;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.HasProto;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import javax.inject.Inject;

import static com.google.common.collect.ImmutableList.toImmutableList;

public class DAOHasProto implements TypedDAO<HasProto> {
  private static final Map<Class<? extends HasProto>, Constructor<? extends HasProto>> defaultConstructors = new HashMap<>();
  private static final Map<Class<? extends HasProto>, Class<? extends Message>> protoTypeMap = new HashMap<>();
  private TypedDAO<Message> messageDAO;

  @Inject
  public DAOHasProto(TypedDAO<Message> messageDAO) {
    this.messageDAO = messageDAO;
  }

  @Override
  public <T extends HasProto> T create(T t) {
    return fromProto((Class<T>) t.getClass(), messageDAO.create(t.toProto()));
  }

  @Override
  public <T extends HasProto> ImmutableList<T> create(Iterable<T> entities) {
    Class<T> c = (Class<T>) entities.iterator().next().getClass();
    return fromProto(c, messageDAO.create(stream(entities).map(T::toProto).collect(toImmutableList())));
  }

  @Override
  public <T extends HasProto, I> T get(Class<T> c, I id) {
    return (T) fromProto(c, messageDAO.get(getProtoType(c), id));
  }

  @Override
  public <T extends HasProto, I> ImmutableList<T> get(Class<T> c, Iterable<I> ids) {
    return fromProto(c, messageDAO.get(getProtoType(c), ids));
  }

  @Override
  public <T extends HasProto> QueryResult<T> list(Class<T> c, Query.List listQuery) {
    QueryResult<? extends Message> result = messageDAO.list(getProtoType(c), listQuery);
    return QueryResult.transform(result, p -> (T) fromProto(c, p));
  }

  @Override
  public <T extends HasProto, I> T update(Class<T> c, I id, UnaryOperator<T> updater) {
    return (T) fromProto(
        c, messageDAO.update(getProtoType(c), id, proto -> updater.apply(fromProto(c, proto)).toProto()));
  }

  @Override
  public <T extends HasProto, I> ImmutableList<T> update(Class<T> c, Iterable<I> ids, UnaryOperator<T> updater) {
    return stream(ids).map(id -> update(c, id, updater)).collect(toImmutableList());
  }

  @Override
  public <T extends HasProto, I> void delete(Class<T> c, I id) {
    messageDAO.delete(getProtoType(c), id);
  }

  @Override
  public <T extends HasProto, I> void delete(Class<T> c, Iterable<I> ids) {
    messageDAO.delete(getProtoType(c), ids);
  }

  private static <T extends HasProto> T fromProto(Class<T> c, Message m) {
    return (T) newInstance(c).fromProto(m);
  }

  private static <T extends HasProto> ImmutableList<T> fromProto(Class<T> c, ImmutableList<Message> messages) {
    return messages.stream().map(m -> fromProto(c, m)).collect(toImmutableList());
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
