package com.digitald4.common.storage;

import com.digitald4.common.proto.DD4Protos.GeneralData;
import javax.inject.Inject;
import javax.inject.Provider;

public class GeneralDataStore extends GenericStore<GeneralData> {
	@Inject
	public GeneralDataStore(Provider<DAO> daoProvider) {
		super(GeneralData.class, daoProvider);
	}

	public QueryResult<GeneralData> listByGroupId(long groupId) {
		return super.list(new Query()
				.setFilters(new Query.Filter().setColumn("GROUP_ID").setOperator("=").setValue(String.valueOf(groupId))));
	}
}
