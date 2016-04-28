package com.digitald4.common.distributed;

public interface Function<R, T> {
	public R execute(T t);
}
