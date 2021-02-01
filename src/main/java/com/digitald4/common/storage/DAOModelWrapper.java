package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.HasProto;
import com.google.protobuf.Message;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import static com.google.common.collect.ImmutableList.toImmutableList;

public class DAOModelWrapper implements DAO<HasProto> {
  private static final Map<Class<? extends HasProto>, Method> fromProtoMap = new HashMap<>();
  private static final Map<Class<? extends HasProto>, Class<? extends Message>> protoTypeMap = new HashMap<>();
  private Provider<DAO<Message>> daoProvider;

  @Inject
  public DAOModelWrapper(Provider<DAO<Message>> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public <T extends HasProto> T create(T t) {
    return fromProto((Class<T>) t.getClass(), daoProvider.get().create(t.toProto()));
  }

  @Override
  public <T extends HasProto> T get(Class<T> c, long id) {
    return (T) fromProto(c, daoProvider.get().get(getProtoType(c), id));
  }

  @Override
  public <T extends HasProto> QueryResult<T> list(Class<T> c, Query query) {
    QueryResult<? extends Message> result = daoProvider.get().list(getProtoType(c), query);
    return new QueryResult<T>(result.getResults().stream().map(p -> (T) fromProto(c, p)).collect(toImmutableList()));
  }

  @Override
  public <T extends HasProto> T update(Class<T> c, long id, UnaryOperator<T> updater) {
    return (T) fromProto(c, daoProvider.get()
        .update(getProtoType(c), id, proto -> updater.apply(fromProto(c, proto)).toProto()));
  }

  @Override
  public <T extends HasProto> void delete(Class<T> c, long id) {
    daoProvider.get().delete(getProtoType(c), id);
  }

  @Override
  public <T extends HasProto> int delete(Class<T> c, Query query) {
    return daoProvider.get().delete(getProtoType(c), query);
  }

  public static <T extends HasProto> T fromProto(Class<T> c, Message m) {
    try {
      return (T) fromProtoMap.computeIfAbsent(c, cls -> {
        try {
          return cls.getMethod("fromProto", getProtoType(cls));
        } catch (NoSuchMethodException | SecurityException e) {
          throw new DD4StorageException("Error getting default instance for type: " + c, e);
        }
      }).invoke(null, m);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
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
