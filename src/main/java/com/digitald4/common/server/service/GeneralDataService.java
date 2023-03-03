package com.digitald4.common.server.service;

import com.digitald4.common.model.GeneralData;
import com.digitald4.common.model.User;
import com.digitald4.common.storage.GeneralDataStore;
import com.digitald4.common.storage.LoginResolver;
import com.digitald4.common.storage.SessionStore;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiNamespace;
import javax.inject.Inject;

@Api(
		name = "generalDatas",
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
public class GeneralDataService extends EntityServiceImpl<GeneralData, Long> {

	@Inject
	public GeneralDataService(GeneralDataStore generalDataStore, LoginResolver loginResolver) {
		super(generalDataStore, loginResolver, false);
	}
}
