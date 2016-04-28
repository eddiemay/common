package com.digitald4.common.util;

public class Threesome<T> {
	private T one;
	
	private T two;
	
	private T three;
	
	public Threesome(T one, T two, T three) {
		this.one = one;
		this.two = two;
		this.three = three;
	}
	
	public T getOne() {
		return one;
	}
	
	public T getTwo() {
		return two;
	}
	
	public T getThree() {
		return three;
	}
	
	@Override
	public String toString() {
		return "(" + one + ", " + two + ", " + three + ")";
	}
}
