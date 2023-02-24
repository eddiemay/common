package com.digitald4.common.storage;

import com.digitald4.common.model.GeneralData;
import javax.inject.Inject;
import javax.inject.Provider;

public class GeneralDataStore extends GenericStore<GeneralData, Long> {
	@Inject
	public GeneralDataStore(Provider<DAO> daoProvider) {
		super(GeneralData.class, daoProvider);
	}

	public QueryResult<GeneralData> listByGroupId(Long groupId) {
		return list(Query.forList().setFilters(Query.Filter.of("groupId", "=", groupId)));
	}
}
