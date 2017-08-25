package com.digitald4.common.jpa;

public class Change implements Comparable<Object>{
	private String property;
	private Object newValue;
	private Object oldValue;
	public Change(String property, Object newValue, Object oldValue){
		this.setProperty(property);
		this.setNewValue(newValue);
		this.setOldValue(oldValue);
	}
	public void setProperty(String property) {
		this.property = property;
	}
	public String getProperty() {
		return property;
	}
	public void setNewValue(Object newValue) {
		this.newValue = newValue;
	}
	public Object getNewValue() {
		return newValue;
	}
	public void setOldValue(Object oldValue) {
		this.oldValue = oldValue;
	}
	public Object getOldValue() {
		return oldValue;
	}
	public String toString(){
		return getProperty();
	}
	@Override
	public int compareTo(Object o) {
		return toString().compareTo(""+o);
	}
}
