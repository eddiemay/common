package com.digitald4.common.jpa;

import javax.persistence.TypedQuery;

public interface DD4TypedQuery<T> extends TypedQuery<T> {

	Class<T> getTypeClass();

	String getSql();

	Object[] getParameterValues();

}
