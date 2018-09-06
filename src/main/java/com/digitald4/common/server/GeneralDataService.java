package com.digitald4.common.server;

import com.digitald4.common.proto.DD4Protos.GeneralData;
import com.digitald4.common.storage.GeneralDataStore;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiNamespace;
import javax.inject.Inject;

@Api(
		name = "generalData",
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
public class GeneralDataService extends SingleProtoService<GeneralData> {

	@Inject
	public GeneralDataService(GeneralDataStore generalDataStore) {
		super(generalDataStore);
	}
}
