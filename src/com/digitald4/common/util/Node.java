package com.digitald4.common.util;

public class Node<T> {
	private T value;
	private Node<T> parent;
	
	public Node(T value, Node<T> parent) {
		this.value = value;
		this.parent = parent;
	}
	
	public Node<T> getParent() {
		return parent;
	}
	
	public int getDepth() {
		return (parent == null) ? 1 : parent.getDepth() + 1;
	}
	
	public String toString() {
		return value.toString();
	}

	public T getValue() {
		return value;
	}
}
