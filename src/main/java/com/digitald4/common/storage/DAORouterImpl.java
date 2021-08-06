package com.digitald4.common.storage;

import static com.digitald4.common.util.JSONUtil.getDefaultInstance;

import com.digitald4.common.model.HasProto;
import com.digitald4.common.storage.Annotations.DefaultDAO;
import com.google.protobuf.Message;;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import javax.inject.Inject;

public class DAORouterImpl implements DAO {
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
  public <T> int delete(Class<T> c, Iterable<Long> ids) {
    Object o = getDefaultInstance(c);

    if (o instanceof Message) {
      return messageDao.delete((Class<Message>) c, ids);
    }

    if (o instanceof HasProto && hasProtoDao != null) {
      return hasProtoDao.delete((Class<HasProto>) c, ids);
    }

    return defaultDao.delete(c, ids);
  }
}
