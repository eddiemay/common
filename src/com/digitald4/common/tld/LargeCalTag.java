package com.digitald4.common.tld;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.joda.time.DateTime;

import com.digitald4.common.component.CalEvent;
import com.digitald4.common.component.Notification;
import com.digitald4.common.util.FormatText;

public class LargeCalTag extends DD4Tag {
	private final static int MAX_EVENT_LINES = 8;
	private final static String START = "<div class=\"block-border\">"
			+ "<div id=\"cal_supp\"></div>"
			+ "<div class=\"block-content\">"
			+ "<h1>Large calendar</h1>"
			+ "<div class=\"block-controls\">"
			+ "<ul class=\"controls-buttons\">"
			+ "<li><img src=\"images/icons/fugue/navigation-180.png\" width=\"16\" height=\"16\" onclick=\"setMonth(%id, %prev_year, %prev_month)\"/></li>"
			+ "<li class=\"sep\"></li>"
			+ "<li class=\"controls-block\"><strong>%month_year</strong></li>"
			+ "<li class=\"sep\"></li>"
			+ "<li><img src=\"images/icons/fugue/navigation.png\" width=\"16\" height=\"16\" onclick=\"setMonth(%id, %next_year, %next_month)\"/></li>"
			+ "</ul>"
			+ "</div>"
			+ "<div class=\"no-margin\">"
			+ "<table cellspacing=\"0\" class=\"calendar\">"
			+ "<thead>"
			+ "<tr><th scope=\"col\" class=\"black-cell\"><span class=\"success\"></span></th>"
			+ "<th scope=\"col\" class=\"week-end\">Sunday</th><th scope=\"col\">Monday</th><th scope=\"col\">Tuesday</th><th scope=\"col\">Wednesday</th>"
			+ "<th scope=\"col\">Thursday</th><th scope=\"col\">Friday</th><th scope=\"col\" class=\"week-end\">Saturday</th>"
			+ "</tr></thead><tbody>";
	private final static String WEEK_START = "<tr><th scope=\"row\">%weeknum</th>";
	private final static String WEEK_END = "</tr>";
	private final static String END = "</tbody></table></div><ul class=\"message no-margin\"><li>%event_count events found</li></ul></div></div>";
	private String title;
	private int userId;
	private String idType;
	private int month;
	private int year;
	private Collection<? extends CalEvent> events;
	private List<Notification<?>> notifications;

	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setIdType(String idType) {
		this.idType = idType;
	}
	
	public String getIdType() {
		return idType;
	}
	
	public void setUserId(int userId) {
		this.userId = userId;
	}
	
	public int getUserId() {
		return userId;
	}
	
	public void setMonth(int month) {
		this.month = month;
	}
	
	public int getMonth() {
		return month;
	}
	
	public void setYear(int year) {
		this.year = year;
	}
	
	public int getYear() {
		return year;
	}
	
	public String getCssClass(DateTime date) {
		if (date.getMonthOfYear() != month) {
			return "other-month";
		}
		DateTime today = DateTime.now();
		if (date.getDayOfYear() == today.getDayOfYear() && date.getYear() == today.getYear()) {
			return "today";
		}
		return "";
	}
	
	public void setEvents(Collection<? extends CalEvent> events) {
		this.events = events;
	}
	
	public Collection<? extends CalEvent> getEvents() {
		return events;
	}
	
	public void setNotifications(List<Notification<?>> notifications) {
		this.notifications = notifications;
	}
	
	public List<Notification<?>> getNotifications() {
		return notifications;
	}
	
	public int getEventCount() {
		return getEvents() != null ? getEvents().size() : 0;
	}
	
	public List<CalEvent> getEvents(DateTime date) {
		List<CalEvent> events = new ArrayList<CalEvent>();
		if (getEvents() != null) {
			for (CalEvent event : getEvents()) {
				if (event.getStart().getDayOfYear() == date.getDayOfYear() && event.getStart().getYear() == date.getYear())
					events.add(event);
			}
		}
		return events;
	}
	
	public List<Notification<?>> getNotifications(DateTime date) {
		List<Notification<?>> notifications = new ArrayList<Notification<?>>();
		if (getNotifications() != null) {
			for (Notification<?> notification : getNotifications()) {
				if (notification.getDate().equals(date.toDate()))
					notifications.add(notification);
			}
		}
		return notifications;
	}
	
	public String getEventStr(DateTime date, short zIndex) {
		List<CalEvent> events = getEvents(date);
		if (events.size() == 0) {
			return "";
		}
		String out = "<ul class=\"events\">";
		int c = 0;
		for (CalEvent event : events) {
			DateTime st = event.getStart();
			if (++c == MAX_EVENT_LINES && events.size() > MAX_EVENT_LINES) {
				out += "</ul><div class=\"more-events\" style=\"z-index: "+ zIndex +";\" >" + (events.size() - c + 1) + " more events<ul>";
			}
			Notification<?> notification = event.getNotification();
			out += "<li><a onclick=\"editEvent(" + event.getId() + ")\">"
			+ (event.isCancelled() ? "<del>" : "")
			+ "<b>" + FormatText.HOUR_MIN.format(st.toDate()) + "</b>" + event.getTitle()
			+ (event.isCancelled() ? "</del>" : "")
			+ (notification == null ? "" : "<img src=\"" + getNotificationIcon(notification.getType()) + "\" width=\"8\" height=\"8\">")
			+ "</a></li>";
		}
		out += "</ul>";
		if (events.size() > MAX_EVENT_LINES) {
			out += "</div>";
		}
		return out;
	}
	
	public static String getNotificationClass(Notification.Type type) {
		switch (type) {
			case ERROR: return "red";
			case WARNING: return "yellow";
			case INFO: return "blue";
			default: return "blue";
		}
	}
	
	public static String getNotificationIcon(Notification.Type type) {
		switch (type) {
			case ERROR: return "images/icons/fugue/flag.png";
			case WARNING: return "images/icons/fugue/exclamation-diamond.png";
			case INFO: return "images/icons/fugue/information-blue.png";
		}
		return "images/icons/fugue/exclamation-diamond.png";
	}
	
	public String getNotificationStr(DateTime date) {
		List<Notification<?>> events = getNotifications(date);
		if (events.size() == 0) {
			return "";
		}
		String out = "<ul class=\"dot-events with-children-tip\">";
		
		for (Notification<?> event : events) {
			out += "<li class=\"" + getNotificationClass(event.getType()) + "\" title=\"" + event.getTitle() + "\"><a href=\"#\">" + event.getTitle() + "</a></li>";
		}
		out += "</ul>";
		return out;
	}
	
	@Override
	public String getOutput() {
		DateTime cal = DateTime.parse(getYear() + "-" + getMonth() + "-01");
		String out = START.replace("%title", getTitle()).replaceAll("%month_year", FormatText.USER_MONTH.format(cal.toDate()))
			.replaceAll("%id", "" + getUserId())
			.replaceAll("%prev_year", ""+(getMonth() > 1 ? getYear() : getYear() - 1)).replaceAll("%prev_month", ""+(getMonth() > 1 ? getMonth() - 1 : 12))
			.replaceAll("%next_year", ""+(getMonth() < 12 ? getYear() : getYear() + 1)).replaceAll("%next_month", ""+(getMonth() < 12 ? getMonth() + 1 : 1));
		DateTime nextMonth = cal.plusMonths(1);
		// Move back to last Sunday
		cal = cal.minusDays(cal.getDayOfWeek() % 7);
		short zIndex = 142;
		while (cal.isBefore(nextMonth)) {
			out += WEEK_START.replaceAll("%weeknum", "" + cal.getWeekOfWeekyear());
			for (int d = 0; d < 7; d++) {
				int day = cal.getDayOfMonth();
				String date = FormatText.formatDate(cal, FormatText.USER_DATE);
				if (d == 0 || d == 6) {
					out += "<td class=\"weekend" + ((cal.getMonthOfYear() == getMonth()) ? "" : " other-month") + "\">";
				} else {
					out += "<td" + ((cal.getMonthOfYear() == getMonth()) ? "" : " class=\"other-month\"") + ">";
				}
				out += "<a href=\"#\" class=\"day\">" + day + "</a>" +
						getNotificationStr(cal) +
						"<div class=\"add-event\" onclick=\"addEvent({'appointment.start_date': '" + date + "', '" + getIdType() + "': " + getUserId() + "})\">Add</div>" +
						getEventStr(cal, zIndex) +
						"</td>";
				cal = cal.plusDays(1);
				zIndex--;
			}
			out += WEEK_END;
		}
		out += END.replaceAll("%event_count", "" + getEventCount());
		return out;
	}
}
