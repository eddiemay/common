package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import java.util.function.UnaryOperator;

public class GenericStore<T> implements Store<T> {

	private final DAO<T> dao;
	
	public GenericStore(DAO<T> dao) {
		this.dao = dao;
	}
	
	@Override
	public T getType() {
		return dao.getType();
	}
	
	@Override
	public T create(T t) throws DD4StorageException {
		return dao.create(t);
	}

	@Override
	public T get(long id) throws DD4StorageException {
		return dao.get(id);
	}

	@Override
	public ListResponse<T> list(ListRequest listRequest) throws DD4StorageException {
		return dao.list(listRequest);
	}

	@Override
	public T update(long id, UnaryOperator<T> updater) throws DD4StorageException {
		return dao.update(id, updater);
	}

	@Override
	public void delete(long id) throws DD4StorageException {
		dao.delete(id);
	}
}
