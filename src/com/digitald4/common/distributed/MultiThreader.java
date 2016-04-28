package com.digitald4.common.distributed;

import java.util.List;

public interface MultiThreader {
	/** Sets the number of runners to use for processing */
	public MultiThreader setRunners(int runners_);
	
	/** Performs the work on the items in parallel using the given function definition */
	public <R, T>  List<R> parDo(List<T> items, Function<R, T> function);
}
