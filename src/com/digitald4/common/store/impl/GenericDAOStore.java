package com.digitald4.common.store.impl;

import java.util.List;

import com.digitald4.common.dao.DAO;
import com.digitald4.common.dao.QueryParam;
import com.digitald4.common.distributed.Function;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.store.DAOStore;

public class GenericDAOStore<T> implements DAOStore<T> {

	private final DAO<T> dao;
	
	public GenericDAOStore(DAO<T> dao) {
		this.dao = dao;
	}
	
	@Override
	public T create(T t) throws DD4StorageException {
		return dao.create(t);
	}

	@Override
	public T read(int id) throws DD4StorageException {
		return dao.read(id);
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
	public List<T> query(QueryParam... params) throws DD4StorageException {
		return dao.query(params);
	}
	
	@Override
	public List<T> getAll() throws DD4StorageException {
		return dao.getAll();
	}
}
