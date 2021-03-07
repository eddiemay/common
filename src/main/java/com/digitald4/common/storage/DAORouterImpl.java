package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.HasProto;
import com.digitald4.common.storage.Annotations.DefaultDAO;
import com.digitald4.common.util.ProtoUtil;
import com.google.protobuf.Message;;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import javax.inject.Inject;

public class DAORouterImpl implements DAO {
  private static final Map<Class<?>, Object> defaultInstances = new HashMap<>();
  private final TypedDAO<Message> messageDao;
  private final TypedDAO<HasProto> hasProtoDao;
  private final DAO defaultDao;

  @Inject
  public DAORouterImpl(
      @Nullable TypedDAO<Message> messageDao,
      @Nullable TypedDAO<HasProto> hasProtoDao,
      @DefaultDAO DAO defaultDao) {
    this.messageDao = messageDao;
    this.hasProtoDao = hasProtoDao;
    this.defaultDao = defaultDao;
  }

  @Override
  public <T> T create(T t) {
    if (t instanceof Message) {
      return (T) messageDao.create((Message) t);
    }

    if (t instanceof HasProto && hasProtoDao != null) {
      return (T) hasProtoDao.create((HasProto) t);
    }

    return defaultDao.create(t);
  }

  @Override
  public <T> T get(Class<T> c, long id) {
    T t = getDefaultInstance(c);

    if (t instanceof Message) {
      return (T) messageDao.get((Class<Message>) c, id);
    }

    if (t instanceof HasProto && hasProtoDao != null) {
      return (T) hasProtoDao.get((Class<HasProto>) c, id);
    }

    return defaultDao.get(c, id);
  }

  @Override
  public <T> QueryResult<T> list(Class<T> c, Query query) {
    T t = getDefaultInstance(c);

    if (t instanceof Message) {
      return (QueryResult<T>) messageDao.list((Class<Message>) c, query);
    }

    if (t instanceof HasProto && hasProtoDao != null) {
      return (QueryResult<T>) hasProtoDao.list((Class<HasProto>) c, query);
    }

    return defaultDao.list(c, query);
  }

  @Override
  public <T> T update(Class<T> c, long id, UnaryOperator<T> updater) {
    T t = getDefaultInstance(c);

    if (t instanceof Message) {
      return (T) messageDao.update((Class<Message>) c, id, current -> (Message) updater.apply((T) current));
    }

    if (t instanceof HasProto && hasProtoDao != null) {
      return (T) hasProtoDao.update((Class<HasProto>) c, id, current -> (HasProto) updater.apply((T) current));
    }

    return defaultDao.update(c, id, updater);
  }

  @Override
  public <T> void delete(Class<T> c, long id) {
    Object t = getDefaultInstance(c);

    if (t instanceof Message) {
      messageDao.delete((Class<Message>) c, id);
      return;
    }

    if (t instanceof HasProto && hasProtoDao != null) {
      hasProtoDao.delete((Class<HasProto>) c, id);
      return;
    }

    defaultDao.delete(c, id);
  }

  @Override
  public <T> int delete(Class<T> c, Query query) {
    Object o = getDefaultInstance(c);

    if (o instanceof Message) {
      return messageDao.delete((Class<Message>) c, query);
    }

    if (o instanceof HasProto && hasProtoDao != null) {
      return hasProtoDao.delete((Class<HasProto>) c, query);
    }

    return defaultDao.delete(c, query);
  }

  static <T> T getDefaultInstance(Class<T> c) {
    return (T) defaultInstances.computeIfAbsent(c, cls -> newInstance(c));
  }

  static <T> T newInstance(Class<T> c) {
    try {
      return c.getConstructor().newInstance();
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      // If this class does not have a default constructor then he may be a proto message.
      try {
        return (T) ProtoUtil.getDefaultInstance((Class<Message>) c);
      } catch (Exception se) {
        throw new DD4StorageException("Error getting default instance for type: " + c, e);
      }
    }
  }
}
