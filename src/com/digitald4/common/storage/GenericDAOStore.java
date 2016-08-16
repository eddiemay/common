package com.digitald4.common.storage;

import java.util.List;

import com.digitald4.common.storage.DAO;
import com.digitald4.common.distributed.Function;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4UIProtos.ListRequest.QueryParam;
import com.digitald4.common.storage.DAOStore;

public class GenericDAOStore<T> implements DAOStore<T> {

	private final DAO<T> dao;
	
	public GenericDAOStore(DAO<T> dao) {
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
	public T get(int id) throws DD4StorageException {
		return dao.get(id);
	}

	@Override
	public T update(int id, Function<T, T> updater) throws DD4StorageException {
		return dao.update(id, updater);
	}

	@Override
	public void delete(int id) throws DD4StorageException {
		dao.delete(id);
	}
	
	@Override
	public List<T> get(QueryParam... params) throws DD4StorageException {
		return dao.get(params);
	}
	
	@Override
	public List<T> get(List<QueryParam> params) throws DD4StorageException {
		return dao.get(params);
	}
	
	@Override
	public List<T> getAll() throws DD4StorageException {
		return dao.getAll();
	}
}
