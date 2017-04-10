package com.digitald4.common.storage;

import com.digitald4.common.proto.DD4Protos.GeneralData;
import com.digitald4.common.proto.DD4UIProtos.ListRequest.QueryParam;
import java.util.List;

public class GeneralDataStore extends GenericStore<GeneralData> {

	private static final QueryParam.Builder BY_GROUP_ID = QueryParam.newBuilder()
			.setColumn("GROUP_ID").setOperan("=");

	public GeneralDataStore(DAO<GeneralData> dao) {
		super(dao);
	}

	public List<GeneralData> listByGroupId(int groupId) {
		return super.get(BY_GROUP_ID.setValue(String.valueOf(groupId)).build());
	}
}
