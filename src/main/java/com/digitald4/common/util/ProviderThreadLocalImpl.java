package com.digitald4.common.util;

import javax.inject.Provider;

public class ProviderThreadLocalImpl<T> implements Provider<T> {
	public final ThreadLocal<T> userThreadLocal = new ThreadLocal<>();
	
	public ProviderThreadLocalImpl set(T t) {
		userThreadLocal.set(t);
		return this;
	}
	
	public T get() {
		return userThreadLocal.get();
	}
}
