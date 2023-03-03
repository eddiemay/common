package com.digitald4.common.server.service;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.storage.ChangeTracker.ChangeHistory;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.LoginResolver;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.QueryResult;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;

@Api(
    name = "changeHistorys",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "dd4common.digitald4.com",
        ownerName = "dd4common.digitald4.com"
    ),
    // [START_EXCLUDE]
    issuers = {
        @ApiIssuer(
            name = "firebase",
            issuer = "https://securetoken.google.com/fantasy-predictor",
            jwksUri =
                "https://www.googleapis.com/service_accounts/v1/metadata/x509/securetoken@system"
                    + ".gserviceaccount.com"
        )
    }
    // [END_EXCLUDE]
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
      return daoProvider.get()
          .list(ChangeHistory.class, Query.forList(filter, orderBy, pageSize, pageToken));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }
}
