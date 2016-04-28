package com.digitald4.common.observer;

import java.util.Vector;

public class Node {
	private int value;
	private Node parent;
	private Vector<Node> nodes = new Vector<Node>();
	private boolean cacheDirty=true;
	private int cachedTotal;
	public Node(int value){
		setValue(value);
	}
	public int getValue(){
		return value;
	}
	public void setValue(int value){
		this.value = value;
		invalidateCache();
	}
	public Node getParent(){
		return parent;
	}
	public void setParent(Node parent){
		this.parent = parent;
	}
	public Vector<Node> getNodes(){
		return nodes;
	}
	public void addNode(Node node){
		getNodes().add(node);
		node.setParent(this);
		invalidateCache();
	}
	public void removeNode(Node node){
		getNodes().remove(node);
		node.setParent(null);
		invalidateCache();
	}
	public boolean isCacheDirty(){
		return cacheDirty;
	}
	public void invalidateCache(){
		cacheDirty=true;
		if(getParent() != null)
			getParent().invalidateCache();
	}
	public int getTotalValueNonCached(){
		int tot=getValue();
		for(Node node:getNodes())
			tot += node.getTotalValueNonCached();
		return tot;
	}
	public synchronized int getTotalValueCached(){
		if(isCacheDirty()){
			cachedTotal = getValue();
			for(Node node:getNodes())
				cachedTotal += node.getTotalValueCached();
			cacheDirty=false;
		}
		return cachedTotal;
	}
}
