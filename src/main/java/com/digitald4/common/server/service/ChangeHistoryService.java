package com.digitald4.common.server.service;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Comparator.comparing;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.server.service.BulkGetable.Ids;
import com.digitald4.common.storage.ChangeTracker.Change;
import com.digitald4.common.storage.ChangeTracker.Change.Type;
import com.digitald4.common.storage.ChangeTracker.ChangeHistory;
import com.digitald4.common.storage.ChangeTracker.ChangeHistory.Action;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.LoginResolver;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Query.OrderBy;
import com.digitald4.common.storage.QueryResult;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Provider;
import org.json.JSONObject;

@Api(
    name = "changeHistorys",
    version = "v1",
    namespace = @ApiNamespace(ownerDomain = "dd4common.digitald4.com", ownerName = "dd4common.digitald4.com")
)
public class ChangeHistoryService implements Listable<ChangeHistory> {
  private final Provider<DAO> daoProvider;
  private final LoginResolver loginResolver;

  @Inject
  public ChangeHistoryService(Provider<DAO> daoProvider, LoginResolver loginResolver) {
    this.daoProvider = daoProvider;
    this.loginResolver = loginResolver;
  }

  @Override
  public Class<ChangeHistory> getTypeClass() {
    return ChangeHistory.class;
  }

  @Override
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "list")
  public QueryResult<ChangeHistory> list(
      @Nullable @Named("filter") String filter, @Nullable @Named("orderBy") String orderBy,
      @Named("pageSize") @DefaultValue("0") int pageSize,
      @Named("pageToken") @DefaultValue("0") int pageToken,
      @Nullable @Named("idToken") String idToken) throws ServiceException {

    try {
      loginResolver.resolve(idToken, true);
      return setDiffs(daoProvider.get()
          .list(ChangeHistory.class, Query.forList(filter, orderBy, pageSize, pageToken)));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "migrateByEntity")
  public AtomicInteger migrateByEntity(@Named("entityIds") String entityIds,
      @Named("entityType") String entityType, @Nullable @Named("idToken") String idToken)
      throws ServiceException {

    return bulkMigrateByEntity(new Ids().setItems(ImmutableSet.copyOf(entityIds.split(","))),
        entityType, idToken);
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "bulkMigrateByEntity")
  public AtomicInteger bulkMigrateByEntity(Ids entityIds, @Named("entityType") String entityType,
      @Nullable @Named("idToken") String idToken) throws ServiceException {
    try {
      loginResolver.resolve(idToken, true);
      DAO dao = daoProvider.get();

      if (entityIds.getItems().isEmpty())
        throw new DD4StorageException("Did not find any entityIds to process", ErrorCode.BAD_REQUEST);

      return new AtomicInteger(
          entityIds.getItems().stream().mapToInt(entityId ->
                  dao.update(ChangeHistory.class,
                      dao.list(ChangeHistory.class,
                              Query
                                  .forList(
                                      Filter.of("entityType", entityType),
                                      Filter.of("entityId", entityId),
                                      Filter.of("action", "CREATED"))
                                  .setOrderBys(OrderBy.of("timeStamp")))
                          .getItems().stream().skip(1).map(ChangeHistory::getId).collect(toImmutableList()),
                      ch -> ch.setAction(Action.MIGRATED)).size())
              .sum());
    } catch (DD4StorageException e) {
      e.printStackTrace();
      throw new ServiceException(e.getErrorCode(), e);
    } catch (Exception e) {
      e.printStackTrace();
      throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
    }
  }

  public static QueryResult<ChangeHistory> setDiffs(QueryResult<ChangeHistory> queryResult) {
    AtomicReference<ChangeHistory> previous = new AtomicReference<>();
    queryResult.getItems().stream().sorted(comparing(ChangeHistory::getTimeStamp)).forEach(entry -> {
      if (previous.get() != null) {
        entry.setChanges(getChanges(
            new JSONObject(entry.getEntity().toString()), new JSONObject(previous.get().getEntity().toString())));
      }
      previous.set(entry);
    });
    return queryResult;
  }

  public static ImmutableList<Change> getChanges(JSONObject curr, JSONObject prev) {
    var adds = curr.keySet().stream().filter(key -> !Objects.equals(curr.get(key), prev.opt(key)))
        .map(key -> {
          Object value = curr.get(key);
          Object prevValue = prev.opt(key);
          if (value instanceof JSONObject && prevValue instanceof JSONObject) {
            return Change.create(key, getChanges((JSONObject) value, (JSONObject) prevValue));
          }
          return Change.create(key, prev.has(key) ? Type.Modified : Type.Add, "" + value, prevValue);
        });
    var removes = prev.keySet().stream()
        .filter(key -> !curr.has(key)).map(key -> Change.create(key, Type.Removed, null, prev.get(key)));

    return Streams.concat(adds, removes).collect(toImmutableList());
  }
}
