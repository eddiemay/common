package com.digitald4.common.storage;

import com.digitald4.common.proto.DD4Protos.GeneralData;
import com.google.protobuf.Message;
import javax.inject.Inject;
import javax.inject.Provider;

public class GeneralDataStore extends ProtoStore<GeneralData> {
	@Inject
	public GeneralDataStore(Provider<DAO<Message>> daoProvider) {
		super(GeneralData.class, daoProvider);
	}

	public QueryResult<GeneralData> listByGroupId(long groupId) {
		return super.list(new Query()
				.setFilters(new Query.Filter().setColumn("GROUP_ID").setOperator("=").setValue(String.valueOf(groupId))));
	}
}
