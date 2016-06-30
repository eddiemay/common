package com.digitald4.common.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.digitald4.common.dao.QueryParam;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.store.DAOStore;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

public class GenericProtoService<T> implements ProtoService<T> {
	
	private final DAOStore<T> store;
	
	public GenericProtoService(DAOStore<T> store) {
		this.store = store;
	}

	@Override
	public T create(Message request) throws DD4StorageException {
		return store.create((T) request.getField(request.getDescriptorForType().findFieldByNumber(1)));
	}

	@Override
	public T get(Message request) throws DD4StorageException {
		return store.read(
				(Integer) request.getField(request.getDescriptorForType().findFieldByName("id")));
	}

	@Override
	public List<T> list(Message request) throws DD4StorageException {
		List<QueryParam> params = new ArrayList<>();
		for (Map.Entry<FieldDescriptor, Object> entry : request.getAllFields().entrySet()) {
			params.add(new QueryParam(entry.getKey().getName(), "=", entry.getValue()));
		}
		return store.query(params);
	}

	@Override
	public T update(Message request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean delete(Message request) throws DD4StorageException {
		store.delete((Integer) request.getField(request.getDescriptorForType().findFieldByName("id")));
		return true;
	}

}
