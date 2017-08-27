package com.digitald4.common.util;

import static java.lang.Thread.sleep;

import java.util.function.Function;

public abstract class RetryableFunction<T, R> implements Function<T, R> {
	private final int tryLimit;

	public RetryableFunction() {
		this(3);
	}

	public RetryableFunction(int tryLimit) {
		this.tryLimit = tryLimit;
	}

	public R applyWithRetries(T t) {
		int failures = 0;
		do {
			try {
				return apply(t);
			} catch (Exception e) {
				e.printStackTrace();
				if (++failures < tryLimit) {
					System.out.println("Retrying in .25 secs...");
					pause();
				} else {
					throw e;
				}
			}
		} while (failures < tryLimit);
		throw new RuntimeException("Out of retries");
	}

	private void pause() {
		try {
			sleep(250);
		} catch (InterruptedException ie) {
			throw new RuntimeException(ie);
		}
	}
}


