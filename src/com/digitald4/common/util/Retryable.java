package com.digitald4.common.util;

import static java.lang.Thread.sleep;

public abstract class Retryable<R, D> {
	private int retryLimit;
	private int failures = 0;
	
	public Retryable() {
		this(1);
	}
	
	public Retryable(int retryLimit) {
		this.retryLimit = retryLimit;
	}
	
	public R run(D data) throws Exception {
		try {
			return execute(data);
		} catch (Exception e) {
			e.printStackTrace();
			if (failures++ < retryLimit) {
				System.out.println("Retrying in 2 secs...");
				sleep(2000);
				return run(data);
			} else {
				throw e;
			}
		}
	}
	
	public abstract R execute(D data) throws Exception;
}
