/*
 * Copyright (c) 2002-2010 ESP Suite. All Rights Reserved.
 *
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 * 
 * Authors: Technology Integration Group, SCE
 * Developers: Eddie Mayfield, Frank Gonzales, Augustin Muniz,
 * Kate Suwan, Hiro Kushida, Andrew McNaughton, Brian Stonerock,
 * Russell Ragsdale, Patrick Ridge, Everett Aragon.
 * 
 */
package com.digitald4.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.sql.Time;
import java.util.Calendar;
import java.util.Collection;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joda.time.DateTime;


/**
 * Calculation utility for rounding, obtaining mva factor,
 * calendar creation, & lat/lon distance calculation.
 */
public class Calculate {

	public static final long ONE_MINUTE = 1000 * 60;

	public static final long ONE_DAY = 60 * ONE_MINUTE * 24;
	
	public static final long LAST_MILLI_OF_DAY = 24 * 60 * 60 * 1000 - 1;
	
	private static MessageDigest md5;

	/**
	 * This method will return a value rounded to 1 decimal place using 
	 * calculate.round if a decimal is to be displayed otherwise it will
	 * return a number without a decimal from math.round
	 * @param number - the number to be rounded
	 * @param displayDecimal - true if a decimal is to be displayed
	 * @return
	 */
	public static Number round(double number, boolean displayDecimal) {
		if(displayDecimal)
			return round(number,1);
		return Math.round(number);
	}

	/**
	 * 
	 * @param n - Double to be rounded
	 * @param p - number of significant figures to right of decimal
	 * @return
	 */
	public static double round(double n, int p){
		n*=Math.pow(10,p);
		n = Math.round(n);
		n/=Math.pow(10,p);
		return n;
	}

	/**
	 * Get the Time 
	 * @param hour
	 * @param min
	 * @param sec
	 * @return a <code>Time</code>Object.
	 */
	public static Time getTime(int hour, int min, int sec){
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY,hour);
		cal.set(Calendar.MINUTE,min);
		cal.set(Calendar.SECOND,sec);
		return new Time(cal.getTimeInMillis());
	}

	/**
	 * 
	 * @param time in the format: "hh:mm:ss"
	 * @return a <code>Time</code>Object.
	 */
	public static Time getTime(String time){
		int hour=0,min=0,sec=0;		
		if(time!=null && time.length()==8){
			char delim = ':';
			hour = Integer.parseInt(time.substring(0,time.indexOf(delim)));
			min = Integer.parseInt(time.substring(time.indexOf(delim)+1,time.lastIndexOf(delim)))-1;
			sec = Integer.parseInt(time.substring(time.lastIndexOf(delim)+1,time.length()));
		}
		return getTime(hour,min,sec);
	}

	/**
	 * 
	 * @param time in the format: "hh:mm:ss"
	 * @return a <code>Calendar</code>Object.
	 */
	public static Calendar getCalendarFromTime(String time){
		char delim = ':';
		int hour = Integer.parseInt(time.substring(0,time.indexOf(delim)));
		int min = Integer.parseInt(time.substring(time.indexOf(delim)+1,time.lastIndexOf(delim)))-1;
		int sec = Integer.parseInt(time.substring(time.lastIndexOf(delim)+1,time.length()));
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY,hour);
		cal.set(Calendar.MINUTE,min);
		cal.set(Calendar.SECOND,sec);
		return cal;
	}	

	public static Calendar zeroOutTime(Calendar c){
		c.set(Calendar.HOUR_OF_DAY,0);
		c.set(Calendar.MINUTE,0);
		c.set(Calendar.SECOND,0);
		return c;
	}	

	public static Calendar getCal(Date date){
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(date.getTime());
		return cal;
	}

	/**
	 * takes date format of "year-month-day"
	 * @param date
	 * @return Calendar
	 */
	public static Calendar getCal(String date){
		char delim = '-';
		int year = Integer.parseInt(date.substring(0,date.indexOf(delim)));
		int month = Integer.parseInt(date.substring(date.indexOf(delim)+1,date.lastIndexOf(delim)))-1;
		int day = Integer.parseInt(date.substring(date.lastIndexOf(delim)+1,date.length()));
		return getCal(year, month, day);
	}

	/**
	 * takes date format of "day/month/yyyy"
	 * @param date
	 * @return Calendar
	 */
	public static Calendar getCal2(String date){
		char delim = '/';
		int day = Integer.parseInt(date.substring(0,date.indexOf(delim)));
		int month = Integer.parseInt(date.substring(date.indexOf(delim)+1,date.lastIndexOf(delim)))-1;
		int year = Integer.parseInt(date.substring(date.lastIndexOf(delim)+1,date.length()));
		return getCal(year, month, day);
	}

	/**
	 * takes date format of "month/day/yy"
	 * @param date
	 * @return Calendar
	 */
	public static Calendar getCal3(String date){
		char delim = '/';
		int month = Integer.parseInt(date.substring(0,date.indexOf(delim)))-1;
		int day = Integer.parseInt(date.substring(date.indexOf(delim)+1,date.lastIndexOf(delim)));
		int year = Integer.parseInt(date.substring(date.lastIndexOf(delim)+1,date.length()));

		if(year>50)
			year+=1900;
		else
			year+=2000;
		return getCal(year, month, day);
	}

	/**
	 * takes date format of MM/DD/YYYY
	 * @param date
	 * @return
	 */
	public static Calendar getCal4(String date){
		char delim = '/';
		int month = Integer.parseInt(date.substring(0,date.indexOf(delim)))-1;
		int day = Integer.parseInt(date.substring(date.indexOf(delim)+1,date.lastIndexOf(delim)));
		int year = Integer.parseInt(date.substring(date.lastIndexOf(delim)+1,date.length()));
		return getCal(year, month, day);
	}	

	/**
	 * 
	 * @return Calendar
	 */
	public static Calendar getCal(){
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY,0);
		cal.set(Calendar.MINUTE,0);
		cal.set(Calendar.SECOND,0);
		cal.set(Calendar.MILLISECOND,0);
		return cal;
	}

	/**
	 * 
	 * @return Calendar
	 */
	public static Calendar getCalYesterday2(){
		Calendar cal = Calendar.getInstance();		
		cal.set(Calendar.HOUR_OF_DAY,22);
		cal.set(Calendar.MINUTE,0);
		cal.set(Calendar.SECOND,0);
		cal.set(Calendar.MILLISECOND,0);
		cal.add(Calendar.DATE, -1);
		return cal;
	}

	/**
	 * 
	 * @return Calendar
	 */
	public static Calendar getCalYesterday(){
		Calendar cal = Calendar.getInstance();		
		cal.set(Calendar.HOUR_OF_DAY,0);
		cal.set(Calendar.MINUTE,0);
		cal.set(Calendar.SECOND,0);
		cal.set(Calendar.MILLISECOND,0);
		cal.add(Calendar.DATE, -1);
		return cal;
	}

	/**
	 * Returns the start of last week.
	 *
	 * @return Calendar
	 */
	public static Calendar getCalLastWeek(){
		Calendar cal = Calendar.getInstance();		
		cal.set(Calendar.HOUR_OF_DAY,0);
		cal.set(Calendar.MINUTE,0);
		cal.set(Calendar.SECOND,0);
		cal.set(Calendar.MILLISECOND,0);
		cal.add(Calendar.DATE, -8); // A week from Yesterday
		return cal;
	}

	/**
	 * 
	 * @param year
	 * @param month
	 * @param day
	 * @return Calendar
	 */
	public static Calendar getCal(int year, int month, int day){
		return getCal(year,month,day,0,0,0);
	}

	/**
	 * 
	 * @param year
	 * @param month
	 * @param day
	 * @param hour
	 * @param min
	 * @param sec
	 * @return Calendar
	 */
	public static Calendar getCal(int year, int month, int day, int hour, int min, int sec){
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR,year);
		cal.set(Calendar.MONTH,month-1);
		cal.set(Calendar.DAY_OF_MONTH,day);
		cal.set(Calendar.HOUR_OF_DAY,hour);
		cal.set(Calendar.MINUTE,min);
		cal.set(Calendar.SECOND,sec);
		cal.set(Calendar.MILLISECOND, 0);
		return (Calendar) cal.clone();
	}

	public static String getDayName(Calendar cal){
		String name="";
		name=FormatText.USER_DAY_OF_WEEK.format(cal.getTime());
		return name;
	}

	public static String getMonthName(Calendar cal){
		String name="";
		name=FormatText.USER_MONTH_ONLY.format(cal.getTime());
		return name;
	}

	/**
	 * Calculates the difference
	 * @param endDate the higher date
	 * @param startDate the lower date
	 * @return the numbers of days difference between the two specified dates.
	 */
	public static int getDaysDiff(Calendar endDate, Calendar startDate){//+1?
		return (int)Math.round((endDate.getTimeInMillis()-startDate.getTimeInMillis())/(1000.0*60*60*24));
	}

	/**
	 * Calculates the difference
	 * @param endDate the higher date
	 * @param startDate the lower date
	 * @return the numbers of days difference between the two specified dates.
	 */
	public static int getHoursDiff(Calendar endDate, Calendar startDate){//+1?
		return (int)Math.round((endDate.getTimeInMillis()-startDate.getTimeInMillis())/(1000.0*60*60));
	}

	/**
	 * Calculates the difference
	 * @param endDate the higher date
	 * @param startDate the lower date
	 * @return the numbers of days difference between the two specified dates.
	 */
	public static int getMinsDiff(Calendar endDate, Calendar startDate){//+1?
		return (int)Math.round((endDate.getTimeInMillis()-startDate.getTimeInMillis())/(1000.0*60*60*60));
	}

	/**
	 * Calculates the distance between two lat/lng points.
	 * 
	 * @param lat1 Start latitude in degrees, minutes, seconds.
	 * @param lng1 Start longitude in degrees, minutes, seconds.
	 * @param lat2 End latitude in degrees, minutes, seconds.
	 * @param lng2 End longitude in degrees, minutes, seconds.
	 * 
	 * @return double The calculated distance in miles.
	 */ 
	public static double distance(double lat1, double lng1, double lat2, double lng2){		
		return Math.toDegrees(Math.acos(Math.sin(Math.toRadians(lat1)) 
				* Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) 
				* Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(lng1 - lng2))))
				* 60 * 1.1515;
	}
	
	/**
	 * @param cal the calendar
	 * @param days the amount of days to increment the calendar by 
	 * @return
	 */
	public static Calendar getCalendarDay(Calendar cal, int days) {		
		Calendar c = (Calendar)cal.clone();
		c.add(Calendar.DATE, days);		
		return c;
	}

	/**
	 * Calculates the SCE Effective Temperature given the required
	 * parameters.
	 * 
	 * @param maxTemp Prior days Max
	 * @param minTemp Prior days Min
	 * @param maxTemp2 2 days Prior Max
	 * @param minTemp2 2 days Prior Min
	 * @param maxTemp3 3 Days Prior Max
	 * @return the calculated effective temperature.
	 */
	public static double calcEffectiveTemp(double maxTemp, double minTemp, double maxTemp2, double minTemp2, double maxTemp3) {
		return .7*maxTemp+.003*minTemp*maxTemp2+.002*minTemp2*maxTemp3;
	}

	public static double calcMaxDeviation(double phaseA, double phaseB, double phaseC) {
		double phaseAve = calcAverage(phaseA,phaseB,phaseC);
		double[] A = {Math.abs(phaseA - phaseAve), Math.abs(phaseB - phaseAve),Math.abs(phaseC - phaseAve)};
		double max = A[0];
		for (int i = 1; i < A.length; i++)
			if (A[i] > max)
				max = A[i];
		return max;
	}

	public static double calcAverage(double value1, double value2, double value3) {		
		return (value1+value2+value3)/3;
	}

	public static double calcMaxDeviationOld(double phase, double phase2, double phase3) {
		double[] A = {Math.abs(phase - phase2), Math.abs(phase - phase3),Math.abs(phase2 - phase3)};
		double max = A[0];
		for(int i = 1; i < A.length; i++)
			if (A[i] > max)
				max = A[i];
		return max;
	}
	public static final double TEN_NANO = .00000001;

	public static boolean isNull(Object o){
		if(o == null) return true;
		if(o instanceof String && ((String)o).length() == 0) return true;
		if(o instanceof Number && ((Number)o).doubleValue() == 0) return true;
		return false;
	}

	//FIXME does not handle errors
	public static boolean isSame(Object o, Object o2){
		try {
			if (o == o2) return true;
			if (isNull(o) && isNull(o2)) return true;
			if (o == null || o2 == null) return false;
			if (o.equals(o2)) return true;
			if (o instanceof Number && o2 instanceof Number && Math.abs(((Number)o).doubleValue()-((Number)o2).doubleValue()) < TEN_NANO) return true;
			if (o instanceof Calendar && o2 instanceof Calendar) 
				return FormatText.USER_DATETIME.format(((Calendar)o).getTime()).equals(FormatText.USER_DATETIME.format(((Calendar)o2).getTime()));
			if (o instanceof Date && o2 instanceof Date)
				return FormatText.USER_DATE.format((Date)o).equals(FormatText.USER_DATE.format((Date)o2));
			if (o instanceof Time && o2 instanceof Time)
				return o.toString().equals(o2.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static double percentMatch(String s, String t){
		if(s == null && t == null)
			return 1;
		if(s == null || t == null)
			return 0;
		if(s.length()+t.length() > 0)
			return 1.0 - (1.0*LD(s,t) / (s.length()+t.length()) );
		return 0;
	}

	//****************************
	// Get minimum of three values
	//****************************
	private static int Minimum (int a, int b, int c) {
		int mi = a;
		if (b < mi)
			mi = b;
		if (c < mi)
			mi = c;
		return mi;
	}

	//*****************************
	// Compute Levenshtein distance
	//*****************************
	public static int LD (String s, String t) {
		int d[][]; // matrix
		int n; // length of s
		int m; // length of t
		int i; // iterates through s
		int j; // iterates through t
		char s_i; // ith character of s
		char t_j; // jth character of t
		int cost; // cost

		// Step 1
		n = s.length ();
		m = t.length ();
		if (n == 0)
			return m;
		if (m == 0) 
			return n;
		d = new int[n+1][m+1];

		// Step 2
		for (i = 0; i <= n; i++)
			d[i][0] = i;

		for (j = 0; j <= m; j++)
			d[0][j] = j;

		// Step 3
		for (i = 1; i <= n; i++) {
			s_i = s.charAt (i - 1);

			// Step 4
			for (j = 1; j <= m; j++) {
				t_j = t.charAt (j - 1);

				// Step 5
				if (s_i == t_j) 
					cost = 0;
				else
					cost = 1;

				// Step 6
				d[i][j] = Minimum (d[i-1][j]+1, d[i][j-1]+1, d[i-1][j-1] + cost);
			}
		}
		// Step 7
		return d[n][m];
	}

	public static double getNPV(int baseYear, double rate, int cost, int year) {
		return cost/Math.pow(1+rate, year-baseYear);
	}

	public static float[] effective(float[] maxs, float[] mins){
		float[] effs = new float[maxs.length];
		float c1=(float).7;
		float c2=(float).003;
		float c3=(float).002;
		for(int x=0; x<maxs.length-2; x++){
			effs[x] = c1*maxs[x]+c2*mins[x]*maxs[x+1]+c3*mins[x+1]*maxs[x+2];
		}
		return effs;
	}

	public static String makeProper(String s) {
		Pattern p = Pattern.compile("(^|\\W)([a-z])");
		Matcher m = p.matcher(s.toLowerCase());
		StringBuffer sb = new StringBuffer(s.length());
		while(m.find())
			m.appendReplacement(sb, m.group(1) + m.group(2).toUpperCase() );
		m.appendTail(sb);
		return sb.toString();
	}
	public static <T> Collection<T> getIntersection(Collection<T>[] collects){
		TreeSet<T> result = new TreeSet<T>(collects[0]);
		for(int i=1; i<collects.length; i++){
			Collection<T> c = collects[i];
			result.retainAll(c);
		}
		return result;
	}
	
	public static String md5(String input) throws NoSuchAlgorithmException {
		if (md5 == null) {
			md5 = MessageDigest.getInstance("MD5");
		}
		return toHex(md5.digest(input.getBytes()));
	}
	
	public static String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
        sb.append(String.format("%02X", b));
    }
		return sb.toString();
	}
	
	public static Pair<DateTime, DateTime> getDateRange(DisplayWindow disWin, DateTime refDate) {
		switch (disWin) {
			case DAY: return Pair.of(refDate, refDate);
			case WEEK: return getWeekRange(refDate);
			case MONTH: return getMonthRange(refDate.getYear(), refDate.getMonthOfYear());
			case CAL_MONTH: return getCalMonthRange(refDate.getYear(), refDate.getMonthOfYear());
			case YEAR: return getYearRange(refDate.getYear());
			case UNKNOWN: return new Pair<DateTime, DateTime>(null, null);
		}
		return new Pair<DateTime, DateTime>(null, null);
	}
	
	public static Pair<DateTime, DateTime> getWeekRange(DateTime refDate) {
		// Move back to first day of the week (Sunday).
 		DateTime start = refDate.minusDays(refDate.getDayOfWeek() % 7).minus(refDate.getMillisOfDay());
		// Move forward to last day of the week (Saturday).
		DateTime end = refDate.plusDays(6 - (refDate.getDayOfWeek() % 7)).plus(LAST_MILLI_OF_DAY);
		return new Pair<DateTime, DateTime>(start, end);
	}
	
	public static Pair<DateTime, DateTime> getMonthRange(int year, int month) {
		// First day of month
		DateTime start = DateTime.parse(year + "-" + month + "-01");
 		// Last day of the month on the last millisecond
		DateTime end = DateTime.parse(year + "-" + month + "-01").plusMonths(1).minus(1);
		return new Pair<DateTime, DateTime>(start, end);
	}
	
	public static Pair<DateTime, DateTime> getCalMonthRange(int year, int month) {
		// Set to first day of month
		DateTime start = DateTime.parse(year + "-" + month + "-01");
		// Move back to first day of the week (Sunday).
 		start = start.minusDays(start.getDayOfWeek() % 7);
 		// Start with the last day of the month on the last millisecond
		DateTime end = DateTime.parse(year + "-" + month + "-01").plusMonths(1).minus(1);
		// Move forward to last day of the week (Saturday).
		end = end.plusDays(6 - (end.getDayOfWeek() % 7));
		return new Pair<DateTime, DateTime>(start, end);
	}
	
	public static Pair<DateTime, DateTime> getYearRange(int year) {
		// Set to first day of month
		DateTime start = DateTime.parse(year + "-01-01");
 		// Start with the last day of the month on the last millisecond
		DateTime end = DateTime.parse(year + "-12-31T23:59:59.999");
		return new Pair<DateTime, DateTime>(start, end);
	}
	
	public static int parseInt(String input) {
		if (input == null || input.length() == 0) {
			return 0;
		}
		return Integer.parseInt(input);
	}
	
	public static double parseDouble(String input) {
		if (input == null || input.length() == 0) {
			return 0;
		}
		return Double.parseDouble(input);
	}

	public static String or(String value, String nullCase) {
		return value != null ? value : nullCase;
	}
}