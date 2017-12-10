package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4UIProtos.CreateRequest;
import com.digitald4.common.proto.DD4UIProtos.DeleteRequest;
import com.digitald4.common.proto.DD4UIProtos.GetRequest;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.proto.DD4UIProtos.ListResponse;
import com.digitald4.common.proto.DD4UIProtos.UpdateRequest;
import com.google.protobuf.Empty;

public interface ProtoService<T> {

	T create(CreateRequest request);
	
	T get(GetRequest request);
	
	ListResponse list(ListRequest request);
	
	T update(UpdateRequest request);
	
	Empty delete(DeleteRequest request);

	boolean requiresLogin(String action);
}
