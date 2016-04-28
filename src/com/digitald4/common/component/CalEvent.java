package com.digitald4.common.component;

import java.util.Date;

import org.joda.time.DateTime;

public interface CalEvent {

	public DateTime getStart();
	
	public DateTime getEnd();
	
	public String getTitle();
	
	public String getDescription();
	
	public int getDuration();
	
	public boolean isActiveOnDay(Date date);
	
	public boolean isActiveBetween(DateTime start, DateTime end);

	public Integer getId();
	
	public boolean isCancelled();
	
	public Notification<CalEvent> getNotification();
}
