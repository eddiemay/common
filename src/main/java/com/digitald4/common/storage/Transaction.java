package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.function.UnaryOperator.identity;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.server.service.BulkGetable;
import com.digitald4.common.util.JSONUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.json.JSONObject;

public class Transaction<T> {
  private ImmutableList<Op<T>> ops = ImmutableList.of();
  private final UnaryOperator<T> updater;
  private final Consumer<Iterable<Op<T>>> prePersist;
  private final Consumer<Iterable<Op<T>>> postPersist;

  private Transaction(UnaryOperator<T> updater,
      Consumer<Iterable<Op<T>>> prePersist, Consumer<Iterable<Op<T>>> postPersist) {
    this.updater = updater;
    this.prePersist = prePersist;
    this.postPersist = postPersist;
  }

  public static <T> Transaction<T> of(Op<T> op) {
    return new Transaction<T>(null, _ops -> {}, _ops -> {}).addOp(op);
  }

  public static <T> Transaction<T> of(Op<T> op, UnaryOperator<T> updater) {
    return new Transaction<T>(updater, _ops -> {}, _ops -> {}).addOp(op);
  }

  public static <T> Transaction<T> of(Op<T> op,
      Consumer<Iterable<Op<T>>> prePersist, Consumer<Iterable<Op<T>>> postPersist) {
    return new Transaction<T>(null, prePersist, postPersist).addOp(op);
  }

  public static <T> Transaction<T> of(Op<T> op, UnaryOperator<T> updater,
      Consumer<Iterable<Op<T>>> prePersist, Consumer<Iterable<Op<T>>> postPersist) {
    return new Transaction<T>(updater, prePersist, postPersist).addOp(op);
  }

  public static <T> Transaction<T> of(Iterable<Op<T>> ops) {
    return new Transaction<T>(null, _ops -> {}, _ops -> {}).addOps(ops);
  }

  public static <T> Transaction<T> of(Iterable<Op<T>> ops, UnaryOperator<T> updater) {
    return new Transaction<T>(updater, _ops -> {}, _ops -> {}).addOps(ops);
  }

  public static <T> Transaction<T> of(Iterable<Op<T>> ops,
      Consumer<Iterable<Op<T>>> prePersist, Consumer<Iterable<Op<T>>> postPersist) {
    return new Transaction<T>(null, prePersist, postPersist).addOps(ops);
  }

  public static <T> Transaction<T> of(Iterable<Op<T>> ops, UnaryOperator<T> updater,
      Consumer<Iterable<Op<T>>> prePersist, Consumer<Iterable<Op<T>>> postPersist) {
    return new Transaction<T>(updater, prePersist, postPersist).addOps(ops);
  }

  public Transaction<T> addOp(Op<T> op) {
    ops = ImmutableList.<Op<T>>builder().addAll(ops).add(op).build();
    return this;
  }

  public Transaction<T> addOps(Iterable<Op<T>> ops) {
    this.ops = ImmutableList.<Op<T>>builder().addAll(this.ops).addAll(ops).build();
    return this;
  }

  public ImmutableList<Op<T>> getOps() {
    return ops;
  }

  public Transaction<T> executeUpdate(DAO dao) {
    Class<T> c = ops.get(0).getTypeClass();

    var ids = ops.stream().filter(op -> updater != null || op.getUpdater() != null)
        .map(Op::getId).collect(toImmutableList());
    BulkGetable.MultiListResult<T, ?> results = dao.get(c, ids);
    if (!results.getMissingIds().isEmpty()) {
      throw new DD4StorageException(
          String.format("One or more items not found while fetching: %s. Requested: %d, found: %d, missing ids: %s",
              c.getSimpleName(), Iterables.size(ids), results.getItems().size(), results.getMissingIds()),
          ErrorCode.NOT_FOUND);
    }
    ops.stream().filter(op -> updater != null || op.getUpdater() != null).forEach(op -> {
      T current = results.getItem(op.getId());
      op.setCurrent(current);
      T entity = JSONUtil.copy(current);
      if (updater != null) {
        entity = updater.apply(entity);
      }
      if (op.getUpdater() != null) {
        entity = op.getUpdater().apply(entity);
      }
      op.setEntity(entity);
    });
    return this;
  }

  public Transaction<T> prePersist() {
    prePersist.accept(ops);
    ops.forEach(Op::prePersist);
    return this;
  }

  public Transaction<T> postPersist() {
    postPersist.accept(ops);
    ops.forEach(Op::postPersist);
    return this;
  }

  public static class Op<T> {
    public enum Action {CREATED, UPDATED, MIGRATED, DELETED}
    private final Class<T> cls;
    private final Action action;
    private Object id;
    private T entity;
    private T current;
    private final UnaryOperator<T> updater;
    private final UnaryOperator<Op<T>> prePersist;
    private final UnaryOperator<Op<T>> postPersist;

    private Op(T entity, Action action, UnaryOperator<T> updater, UnaryOperator<Op<T>> prePersist, UnaryOperator<Op<T>> postPersist) {
      this.cls = (Class<T>) entity.getClass();
      this.action = action;
      setEntity(entity);
      this.updater = updater;
      this.prePersist = prePersist;
      this.postPersist = postPersist;
    }

    private Op(Class<T> cls, Action action, Object id, UnaryOperator<T> updater, UnaryOperator<Op<T>> prePersist, UnaryOperator<Op<T>> postPersist) {
      this.cls = cls;
      this.action = action;
      this.id = id;
      this.updater = updater;
      this.prePersist = prePersist;
      this.postPersist = postPersist;
    }

    public static <E> Op<E> create(E entity) {
      return create(entity, identity(), identity());
    }

    public static <E> Op<E> create(E entity, UnaryOperator<Op<E>> preprocess, UnaryOperator<Op<E>> postprocess) {
      return new Op<>(entity, Action.CREATED, null, preprocess, postprocess);
    }

    public static <E> Op<E> update(Class<E> cls, Object id, UnaryOperator<E> updater) {
      return update(cls, id, updater, identity(), identity());
    }

    public static <E> Op<E> update(Class<E> cls, Object id, UnaryOperator<E> updater, UnaryOperator<Op<E>> preprocess, UnaryOperator<Op<E>> postprocess) {
      return new Op<>(cls, Action.UPDATED, id, updater, preprocess, postprocess);
    }

    public static <E> Op<E> migrate(Class<E> cls, Object id, UnaryOperator<E> updater, UnaryOperator<Op<E>> preprocess, UnaryOperator<Op<E>> postprocess) {
      return new Op<>(cls, Action.MIGRATED, id, updater, preprocess, postprocess);
    }

    public static <E> Op<E> migrate(E entity) {
      return new Op<>(entity, Action.MIGRATED, identity(), identity(), identity());
    }

    public static <E> Op<E> migrate(E entity, UnaryOperator<Op<E>> preprocess, UnaryOperator<Op<E>> postprocess) {
      return new Op<>(entity, Action.MIGRATED, identity(), preprocess, postprocess);
    }

    public static <T> Op<T> deleted(T item) {
      return new Op<>(item, Action.DELETED, null, null, null);
    }

    public Class<T> getTypeClass() {
      return cls;
    }

    public Action getAction() {
      return action;
    }

    public Op<T> setId(Object id) {
      this.id = id;
      return this;
    }

    public Op<T> setEntity(T entity) {
      this.entity = entity;
      if (id == null) {
        id = new JSONObject(entity).opt("id");
      }
      return this;
    }

    public Op<T> setCurrent(T current) {
      this.current = current;
      return this;
    }

    public T getEntity() {
      return entity;
    }

    public T getCurrent() {
      return current;
    }

    public Object getId() {
      return id;
    }

    public UnaryOperator<T> getUpdater() {
      return updater;
    }

    public Op<T> prePersist() {
      prePersist.apply(this);
      return this;
    }

    public Op<T> postPersist() {
      postPersist.apply(this);
      return this;
    }
  }
}
