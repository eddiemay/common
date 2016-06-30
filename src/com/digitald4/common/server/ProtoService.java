package com.digitald4.common.server;

import java.util.List;

import com.digitald4.common.exception.DD4StorageException;
import com.google.protobuf.Message;

public interface ProtoService<T> {
	
	T create(Message request) throws DD4StorageException;
	
	T get(Message request) throws DD4StorageException;
	
	<R extends Message> List<T> list(R request) throws DD4StorageException;
	
	<R extends Message> T update(R request) throws DD4StorageException;
	
	<R extends Message> boolean delete(R request) throws DD4StorageException;
}
