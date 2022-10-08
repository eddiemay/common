package com.digitald4.common.server.service;

import com.digitald4.common.model.GeneralData;
import com.digitald4.common.model.User;
import com.digitald4.common.storage.GeneralDataStore;
import com.digitald4.common.storage.SessionStore;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiNamespace;
import javax.inject.Inject;

@Api(
		name = "generalDatas",
		version = "v1",
		namespace = @ApiNamespace(
				ownerDomain = "nbastats.digitald4.com",
				ownerName = "nbastats.digitald4.com"
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
public class GeneralDataService<U extends User> extends EntityServiceImpl<GeneralData, Long> {

	@Inject
	public GeneralDataService(GeneralDataStore generalDataStore, SessionStore<U> sessionStore) {
		super(generalDataStore, sessionStore, false);
	}
}
