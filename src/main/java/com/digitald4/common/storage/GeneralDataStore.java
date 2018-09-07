package com.digitald4.common.storage;

import com.digitald4.common.proto.DD4Protos.GeneralData;
import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.proto.DD4Protos.Query.Filter;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Provider;

public class GeneralDataStore extends GenericStore<GeneralData> {

	private static final Query.Filter BY_GROUP_ID = Filter.newBuilder()
			.setColumn("GROUP_ID").setOperator("=").build();

	@Inject
	public GeneralDataStore(Provider<DAO> daoProvider) {
		super(GeneralData.class, daoProvider);
	}

	public QueryResult<GeneralData> listByGroupId(long groupId) {
		return super.list(Query.newBuilder()
				.addFilter(BY_GROUP_ID.toBuilder().setValue(String.valueOf(groupId)))
				.build());
	}
}
