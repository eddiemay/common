package com.digitald4.common.component;

import java.util.Date;

public class Notification<E> {
	public enum Type{INFO, WARNING, ERROR};
	private String title;
	private Date date;
	private Type type;
	private E refObj;
	
	public Notification(String title, Date date, Type type, E refObj) {
		this.title = title;
		this.date = date;
		this.type = type;
		this.refObj = refObj;
	}
	
	public String getTitle() {
		return title;
	}
	
	public Date getDate() {
		return date;
	}
	
	public Type getType() {
		return type;
	}
	
	public boolean isBetween(Date start, Date end) {
		return !start.after(getDate()) && !end.before(getDate());
	}

	public E getRefObj() {
		return refObj;
	}
	
	public String toString() {
		return getTitle();
	}
}
