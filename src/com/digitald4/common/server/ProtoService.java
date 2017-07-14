package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4UIProtos.CreateRequest;
import com.digitald4.common.proto.DD4UIProtos.DeleteRequest;
import com.digitald4.common.proto.DD4UIProtos.GetRequest;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.proto.DD4UIProtos.UpdateRequest;
import com.digitald4.common.storage.ListResponse;
import com.google.protobuf.Empty;

public interface ProtoService<T> {

	T create(CreateRequest request) throws DD4StorageException;
	
	T get(GetRequest request) throws DD4StorageException;
	
	ListResponse<T> list(ListRequest request) throws DD4StorageException;
	
	T update(UpdateRequest request) throws DD4StorageException;
	
	Empty delete(DeleteRequest request) throws DD4StorageException;

	boolean requiresLogin(String action);
}
