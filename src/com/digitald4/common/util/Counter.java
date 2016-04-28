package com.digitald4.common.util;

public class Counter {
	private int count = 0;
	
	public Counter increment() {
		++count;
		return this;
	}
	
	public Counter decrement() {
		--count;
		return this;
	}
	
	public int getValue() {
		return count;
	}
	
	@Override
	public String toString() {
		return Integer.toString(count);
	}
}
