package com.digitald4.common.server;

import java.util.List;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4UIProtos.DeleteRequest;
import com.digitald4.common.proto.DD4UIProtos.GetRequest;
import com.digitald4.common.proto.DD4UIProtos.UpdateRequest;
import com.google.protobuf.Message;

public interface ProtoService<T> {
	
	T create(Message request) throws DD4StorageException;
	
	T get(GetRequest request) throws DD4StorageException;
	
	List<T> list(Message request) throws DD4StorageException;
	
	T update(UpdateRequest request) throws DD4StorageException;
	
	boolean delete(DeleteRequest request) throws DD4StorageException;
}
