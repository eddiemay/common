package com.digitald4.common.util;

public class Pair<L, R> implements Comparable<Pair<L, R>> {
	public enum Side{LEFT, RIGHT};

	private final L left;
	private final R right;
	private final Side firstCompare;
	
	private Pair(L left, R right) {
		this(left, right, Side.LEFT);
	}
	
	private Pair(L left, R right, Side firstCompare) {
		this.left = left;
		this.right = right;
		this.firstCompare = firstCompare;
	}
	
	public static <L, R> Pair<L, R> of(L left, R right) {
		return new Pair<L, R>(left, right);
	}
	
	public L getLeft() {
		return left;
	}
	
	public R getRight() { 
		return right;
	}

	@Override
	@SuppressWarnings("unchecked")
	public int compareTo(Pair<L, R> o) {
		int ret = 0;
		if (firstCompare == Side.RIGHT) {
			ret = ((Comparable<R>)getRight()).compareTo(o.getRight());
			if (ret == 0) {
				ret = ((Comparable<L>)getLeft()).compareTo(o.getLeft());
			}
		} else {
			ret = ((Comparable<L>)getLeft()).compareTo(o.getLeft());
			if (ret == 0) {
				ret = ((Comparable<R>)getRight()).compareTo(o.getRight());
			}
		}
		return ret;
	}
	
	@Override
	public String toString() {
		return "(" + getLeft() + ", " + getRight() + ")";
	}
}
