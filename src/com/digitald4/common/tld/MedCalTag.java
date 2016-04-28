package com.digitald4.common.tld;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.joda.time.DateTime;

import com.digitald4.common.component.CalEvent;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.FormatText;

public class MedCalTag extends DD4Tag {
	private final static String START = "<div class=\"block-border\"><div class=\"block-content\">"
				+ "<h1>%title</h1><div class=\"box\"><p class=\"mini-infos\"><strong>%title</strong></p></div>"
				+ "<div class=\"medium-calendar\">"
				+ "<div class=\"calendar-controls\">"
				+ "<input class=\"calendar-prev\" alt=\"Prev month\" onclick=\"setMonth(%prev_year, %prev_month)\" type=\"image\" src=\"images/cal-arrow-left.png\" width=\"16\" height=\"16\"/>"
				+ "<input class=\"calendar-next\" alt=\"Next month\" onclick=\"setMonth(%next_year, %next_month)\" type=\"image\" src=\"images/cal-arrow-right.png\" width=\"16\" height=\"16\"/>"
				+ "%month_year"
				+ "</div>"
				+ "<table cellspacing=\"0\">"
				+ "<thead><tr>"
				+ "<th scope=\"col\" class=\"week-end\">Sun</th><th scope=\"col\">Mon</th><th scope=\"col\">Tue</th>"
				+ "<th scope=\"col\">Wed</th><th scope=\"col\">Thu</th><th scope=\"col\">Fri</th><th scope=\"col\" class=\"week-end\">Sat</th>"
				+ "</tr></thead>"
				+ "<tbody>";
	private final static String ROW = "<tr><td class=\"week-end %class1\"><a href=\"appointment?dt=%dt1%new_app_ids\">%day1%event1</a></td>"
				+ "<td class=\"%class2\"><a href=\"appointment?dt=%dt2%new_app_ids\">%day2%event2</a></td><td class=\"%class3\"><a href=\"appointment?dt=%dt2%new_app_ids\">%day3%event3</a></td>"
				+ "<td class=\"%class4\"><a href=\"appointment?dt=%dt4%new_app_ids\">%day4%event4</a></td><td class=\"%class5\"><a href=\"appointment?dt=%dt5%new_app_ids\">%day5%event5</a></td>"
				+ "<td class=\"%class6\"><a href=\"appointment?dt=%dt6%new_app_ids\">%day6%event6</a></td><td class=\"week-end %class7\"><a href=\"appointment?dt=%dt7%new_app_ids\">%day7%event7</a></td>"
				+ "</tr>";
	private final static String END = "</tbody></table></div></div></div>";
	private String title;
	private int month;
	private int year;
	private Collection<? extends CalEvent> events;
	private String newAppIds = "";

	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return title;
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
	
	public String getCssClass(Calendar cal) {
		if (cal.get(Calendar.MONTH) + 1 != month) {
			return "other-month";
		}
		Calendar today = Calendar.getInstance();
		if (cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) && cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
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
	
	public List<CalEvent> getEvents(Calendar cal) {
		List<CalEvent> events = new ArrayList<CalEvent>();
		if(getEvents() != null) {
			for (CalEvent event : getEvents()) {
				if (event.isActiveOnDay(cal.getTime()))
					events.add(event);
			}
		}
		return events;
	}
	
	public String getEventStr(Calendar cal) {
		List<CalEvent> events = getEvents(cal);
		if (events.size() == 1) {
			DateTime st = events.get(0).getStart();
			return "<span class=\"nb-events\">"+FormatText.HOUR_MIN.format(st.toDate())+"</span>";
		}
		if (events.size() > 1) {
			return "<span class=\"nb-events\">"+events.size()+"</span>";
		}
		return "";
	}
	
	public String getNewAppIds() {
		return newAppIds;
	}
	
	public void setNewAppIds(String newAppIds) {
		this.newAppIds  = newAppIds;
	}
	
	private String getNewAppDt(Calendar cal) {
		return FormatText.formatDate(cal, FormatText.MYSQL_DATETIME);
	}
	
	@Override
	public String getOutput() {
		Calendar cal = Calculate.getCal(getYear(), getMonth(), 1);
		String out = START.replace("%title", getTitle()).replaceAll("%month_year", FormatText.USER_MONTH.format(cal.getTime()))
			.replaceAll("%prev_year", ""+(getMonth() > 1 ? getYear() : getYear() - 1)).replaceAll("%prev_month", ""+(getMonth() > 1 ? getMonth() - 1 : 12))
			.replaceAll("%next_year", ""+(getMonth() < 12 ? getYear() : getYear() + 1)).replaceAll("%next_month", ""+(getMonth() < 12 ? getMonth() + 1 : 1));
		cal.add(Calendar.DATE, Calendar.SUNDAY - cal.get(Calendar.DAY_OF_WEEK));
		Calendar nextMonth = Calculate.getCal(getYear(), getMonth(), 1);
		nextMonth.add(Calendar.MONTH, 1);
		while (cal.getTimeInMillis() < nextMonth.getTimeInMillis()) {
			out += ROW.replaceAll("%new_app_ids", getNewAppIds() != null && getNewAppIds().length() > 0 ? "&" + getNewAppIds() : "");
			for (int d=1; d<=7; d++) {
				out = out.replaceAll("%day"+d, ""+cal.get(Calendar.DAY_OF_MONTH)).replaceAll("%class"+d, getCssClass(cal)).replaceAll("%event"+d, getEventStr(cal))
						.replaceAll("%dt"+d, getNewAppDt(cal));
				cal.add(Calendar.DATE, 1);
			}
		}
		out += END;
		return out;
	}
}
