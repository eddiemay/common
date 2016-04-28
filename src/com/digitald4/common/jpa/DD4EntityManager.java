package com.digitald4.common.jpa;

import java.util.List;

import javax.persistence.EntityManager;

public interface DD4EntityManager extends EntityManager {
	public <T> List<T> fetchResults(DD4TypedQuery<T> tq) throws Exception;
}
