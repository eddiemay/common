package com.digitald4.common.storage;

import com.digitald4.common.model.GeneralData;
import javax.inject.Inject;
import javax.inject.Provider;

public class GeneralDataStore extends GenericStore<GeneralData> {
	@Inject
	public GeneralDataStore(Provider<DAO> daoProvider) {
		super(GeneralData.class, daoProvider);
	}

	public QueryResult<GeneralData> listByGroupId(long groupId) {
		return super.list(Query.forList().setFilters(Query.Filter.of("GROUP_ID", "=", String.valueOf(groupId))));
	}
}
