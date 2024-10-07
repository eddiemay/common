package com.digitald4.common.server.service;

import com.digitald4.common.model.Flag;
import com.digitald4.common.storage.LoginResolver;
import com.digitald4.common.storage.Store;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiNamespace;
import javax.inject.Inject;

@Api(
    name = "flags",
    version = "v1",
    namespace = @ApiNamespace(ownerDomain = "common.digitald4.com", ownerName = "common.digitald4.com")
)
public class FlagService extends EntityServiceImpl<Flag, String> {
  @Inject
  public FlagService(Store<Flag, String> flagStore, LoginResolver loginResolver) {
    super(flagStore, loginResolver);
  }

  @Override
  protected boolean requiresLogin(String method) {
    return !"list".equals(method);
  }
}
