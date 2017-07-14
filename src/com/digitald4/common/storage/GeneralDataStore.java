package com.digitald4.common.storage;

import com.digitald4.common.proto.DD4Protos.GeneralData;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.proto.DD4UIProtos.ListRequest.Filter;
import java.util.List;

public class GeneralDataStore extends GenericStore<GeneralData> {

	private static final Filter.Builder BY_GROUP_ID = Filter.newBuilder()
			.setColumn("GROUP_ID").setOperan("=");

	public GeneralDataStore(DAO<GeneralData> dao) {
		super(dao);
	}

	public List<GeneralData> listByGroupId(int groupId) {
		return super.list(ListRequest.newBuilder()
				.addFilter(BY_GROUP_ID.setValue(String.valueOf(groupId)))
				.build()).getItemsList();
	}
}
