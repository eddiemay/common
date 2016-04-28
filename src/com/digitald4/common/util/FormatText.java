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

import java.sql.Time;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.joda.time.DateTime;


/** 
 * This class will be used to return text to the calling class 
 * in a pre-determined format.  
 * 
 * @author Distribution Staff Engineering
 * @version 2.0
 */ 
public class FormatText {
	public final static SimpleDateFormat DB_DATE = new SimpleDateFormat("dd-MMM-yy");
	public final static SimpleDateFormat MYSQL_DATE = new SimpleDateFormat("yyyy-MM-dd");
	public final static SimpleDateFormat MYSQL_DATETIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public final static SimpleDateFormat BUILD_DATE = new SimpleDateFormat("yyyyMMdd");
	public final static SimpleDateFormat DB_DATETIME = new SimpleDateFormat("dd-MMM-yy HH:mm:ss");
	public final static SimpleDateFormat NOTIFICATION = new SimpleDateFormat("yyyyddMMHHmmss");
	public final static SimpleDateFormat USER_DATE = new SimpleDateFormat("MM/dd/yyyy");
	public final static SimpleDateFormat USER_DATE_SHORT = new SimpleDateFormat("EEE MM/dd/yy");
	public final static SimpleDateFormat USER_DATE_LONG = new SimpleDateFormat("EEE MM/dd/yyyy");
	public final static SimpleDateFormat USER_MONTH = new SimpleDateFormat("MMM yyyy");
	public final static SimpleDateFormat USER_MONTH_ONLY = new SimpleDateFormat("MMM");
	public final static SimpleDateFormat USER_DATETIME = new SimpleDateFormat("MM/dd/yyyy HH:mm");
	public final static SimpleDateFormat USER_DATETIME_SHORT = new SimpleDateFormat("MM/dd/yy HH:mm");
	public final static SimpleDateFormat USER_TIME = new SimpleDateFormat("HH:mm");
	public final static SimpleDateFormat USER_DAY_OF_WEEK = new SimpleDateFormat("EEE");
	public final static SimpleDateFormat USER_DOW_DATE = new SimpleDateFormat("EEE MM/dd/yyyy");
	public final static SimpleDateFormat USER_DOW_DATETIME_SHORT = new SimpleDateFormat("EEE MM/dd/yy HH:mm");
	public final static SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss");
	public final static SimpleDateFormat HOUR_MIN = new SimpleDateFormat("HH:mm");
	public final static SimpleDateFormat SHORT_DATE = new SimpleDateFormat("MM/dd/yy");
	public final static DecimalFormat DECIMAL = new DecimalFormat("###.##");
//	public final static DecimalFormat CURRENCY = new DecimalFormat("$###,###.##;($###,###.##)");
	public final static NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);
	public final static NumberFormat NUMBER = NumberFormat.getInstance();
	public final static DecimalFormat PERCENT = new DecimalFormat("##.##%;(##.##%)");
	
	public final static long ONE_SEC = 1000;
	public final static long ONE_MIN = ONE_SEC * 60;
	public final static long ONE_HOUR = ONE_MIN * 60;
	

	/** This method converts an input string into proper format by setting
	 * the first charater to uppercase and all following characters into
	 * lowercase.  If the string contains a non letter character, it will convert
	 * the trailing text into proper format as well.  This wethod will typically
	 * be used for converting meta data column names into proper format.
	 * 
	 * @return String The string in Proper format.
	 */ 
	public static String printProper( String str ) {
		String letter="";
		char ch;       // One of the characters in str.
		char prevCh;   // The character that comes before ch in the string.
		int i;         // A position in str, from 0 to str.length()-1.
		prevCh = '.';  // Prime the loop with any non-letter character.
		for ( i = 0;  i < str.length();  i++ ) {
			ch = str.charAt(i);
			if ( Character.isLetter(ch)  &&  ! Character.isLetter(prevCh) )
				letter+=Character.toUpperCase(ch);
			else
				letter+=Character.toLowerCase(ch);
			prevCh = ch;
		}
		return letter;
	}
	public static String toSpaced(String camelCase){
		String spaced="";
		char ch;       // One of the characters in str.
		if(camelCase.length() > 0)
			spaced += Character.toUpperCase(camelCase.charAt(0));
		for (int i = 1;  i < camelCase.length(); i++){
			ch = camelCase.charAt(i);
			if (Character.isLetter(ch) && Character.isUpperCase(ch))
				spaced+=" "+ch;
			else
				spaced+=ch;
		}
		return spaced;
	}

	/** This method converts an input string into proper format by setting
	 * the first charater to uppercase, all following characters will be left
	 * as is.
	 * 
	 * @return String The string in Capitalized format.
	 */ 
	public static String printCapitalized( String str ) {
		String letter="";
		char ch;       // One of the characters in str.
		char prevCh;   // The character that comes before ch in the string.
		int i;         // A position in str, from 0 to str.length()-1.
		prevCh = '.';  // Prime the loop with any non-letter character.
		for ( i = 0;  i < str.length();  i++ ) {
			ch = str.charAt(i);
			if ( Character.isLetter(ch)  &&  ! Character.isLetter(prevCh) )
				letter+=Character.toUpperCase(ch);
			else
				letter+=ch;
			prevCh = ch;
		}
		return letter;
	}

	/** This method converts an input string into all uppercase.
	 * Non-letter character will be left as-is.
	 * 
	 * @return String The string in CAPS format.
	 */ 
	public static String printCaps( String str ) {
		String letter="";
		char ch;       // One of the characters in str.
		char prevCh;   // The character that comes before ch in the string.
		int i;         // A position in str, from 0 to str.length()-1.
		prevCh = '.';  // Prime the loop with any non-letter character.
		for ( i = 0;  i < str.length();  i++ ) {
			ch = str.charAt(i);
			if ( Character.isLetter(ch)  &&  ! Character.isLetter(prevCh) )
				letter+=Character.toUpperCase(ch);
			else
				letter+=Character.toUpperCase(ch);
			prevCh = ch;
		}
		return letter;
	}

	/**
	 * 
	 * @param str
	 * @return
	 */
	public static String toLowerCamel(String str){
		String letter="";
		char ch;       // One of the characters in str.
		char prevCh;   // The character that comes before ch in the string.
		int i;         // A position in str, from 0 to str.length()-1.
		prevCh = 'a';  // Prime the loop with any letter character.
		for ( i = 0;  i < str.length();  i++ ) {
			ch = str.charAt(i);
			if ( Character.isLetter(ch)  &&  ! Character.isLetter(prevCh) )
				letter+=Character.toUpperCase(ch);
			else if(Character.isLetter(ch))
				letter+=Character.toLowerCase(ch);
			else if(Character.isDigit(ch))
				letter+=ch;
			prevCh = ch;
		}
		return letter;
	}

	/**
	 * 
	 * @param str
	 * @return
	 */
	public static String toUpperCamel(String str){
		String letter="";
		char ch;       // One of the characters in str.
		char prevCh;   // The character that comes before ch in the string.
		int i;         // A position in str, from 0 to str.length()-1.
		prevCh = '.';  // Prime the loop with any non-letter character.
		for ( i = 0;  i < str.length();  i++ ) {
			ch = str.charAt(i);
			if (Character.isLetter(ch)){
				if(!Character.isLetter(prevCh))
					letter+=Character.toUpperCase(ch);
				else
					letter+=Character.toLowerCase(ch);
			}
			else if(ch!=' ' && ch!='_' && ch!='.')
				letter+=ch;
			prevCh = ch;
		}
		return letter;
	}

	/**
	 * 
	 * @param day
	 * @param month
	 * @param year
	 * @return
	 */
	public static Calendar setCalendarDate( int day, int month, int year ) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DAY_OF_MONTH,day);
		cal.set(Calendar.MONTH,month);
		cal.set(Calendar.YEAR,year);
		return cal;
	}

	public static int getCalendarDay(Calendar cal){
		return cal.get(Calendar.DAY_OF_MONTH);		
	}

	public static int getCalendarMonth(Calendar cal){
		return cal.get(Calendar.MONTH)+1;		
	}

	public static int getCalendarYear(Calendar cal){
		return cal.get(Calendar.YEAR);		
	}

	public static int getCalendarWeekDay(Calendar cal){
		return cal.get(Calendar.DAY_OF_WEEK);
	}

	public static String formatTime(Calendar cal){
		if(cal==null)
			return "00:00";
		return USER_TIME.format(cal.getTime());
	}
	
	public static String formatTime(DateTime dateTime) {
		if(dateTime == null) {
			return null;
		}
		return USER_TIME.format(dateTime.toDate());
	}
	
	public static String formatCurrency(double dollarAmount) {
		return CURRENCY.format(dollarAmount);
	}

	public static String cleanForHtml(String in){

		if(in==null)
			return "";

		char i = (char)10; //decimal 10 = ASCII \n

		if(in.contains(""+i))
			return in.replace(i,' ');

		return in.replaceAll("'", "");

	}
	
	public static String formatDate(Calendar date, SimpleDateFormat dateformat) {
		if (date == null) {
			return null;
		}
		return dateformat.format(date.getTime());
	}
	
	public static String formatDate(Date date, SimpleDateFormat dateformat) {
		if (date == null) {
			return null;
		}
		return dateformat.format(date);
	}
	
	public static String formatDate(Calendar date) {
		return formatDate(date,USER_DATETIME);
	}
	
	public static String formatDate(Date date) {
		return formatDate(date,USER_DATE);
	}
	
	public static Date parseDate(String value) throws ParseException {
		return parseDate(value, USER_DATE);
	}
	
	public static Date parseDate(String value, SimpleDateFormat format) throws ParseException {
		if (value == null || value.length()==0)
			return null;
		return format.parse(value);
	}
	public static Time parseTime(String value) throws ParseException {
		return parseTime(value, USER_TIME);
	}
	
	private static Time parseTime(String value, SimpleDateFormat userTime) throws ParseException {
		return new Time(userTime.parse(value).getTime());
	}
	public static String formatDate(DateTime dateTime) {
		return formatDate(dateTime, USER_DATE);
	}
	public static String formatDate(DateTime dateTime, SimpleDateFormat format) {
		if (dateTime == null)
			return null;
		return formatDate(dateTime.toDate(), format);
	}
	
	public static String formatElapshed(long millis) {
		int hours = (int) (millis / ONE_HOUR);
		int mins = (int) (millis % ONE_HOUR / ONE_MIN);
		int secs = (int) (millis % ONE_MIN / ONE_SEC);
		String result = "";
		if (hours > 0) {
			result += hours + "h";
		}
		if (mins > 0 || result.length() > 0) {
			result += mins + "m";
		}
		if (secs > 0 || result.length() > 0) {
			result += secs + "s";
		} else {
			result = millis + "ms";
		}
		return result;
	}
}
