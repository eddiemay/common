package com.digitald4.common.util;

public class Fibonacci {
	public static int[] genSequence(int length) {
		int[] fibonacci = new int[length];
		int x = 0;
		int last = 1;
		for (int i = 0; i < length; i++) {
			x += last;
			last = x - last;
			fibonacci[i] = x;
		}
		return fibonacci;
	}
}
