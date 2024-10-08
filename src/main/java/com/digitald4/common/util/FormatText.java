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

import com.google.common.collect.ImmutableList;
import java.sql.Time;
import java.text.*;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import java.util.TimeZone;
import org.joda.time.DateTime;

/**
 * This class will be used to return text to the calling class in a pre-determined format.
 *
 * @author Distribution Staff Engineering
 * @version 2.0
 */
public class FormatText {
  public final static SimpleDateFormat DB_DATE = createSimpleDateFormat("dd-MMM-yy");
  public final static SimpleDateFormat MYSQL_DATE = createSimpleDateFormat("yyyy-MM-dd");
  public final static SimpleDateFormat MYSQL_DATETIME = createSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  public final static SimpleDateFormat BUILD_DATE = createSimpleDateFormat("yyyyMMdd");
  public final static SimpleDateFormat BUILD_DATETIME = createSimpleDateFormat("yyyyMMdd_HHmmss");
  public final static SimpleDateFormat DB_DATETIME = createSimpleDateFormat("dd-MMM-yy HH:mm:ss");
  public final static SimpleDateFormat NOTIFICATION = createSimpleDateFormat("yyyyddMMHHmmss");
  public final static SimpleDateFormat USER_DATE = createSimpleDateFormat("MM/dd/yyyy");
  public final static SimpleDateFormat USER_DATE_SHORT = createSimpleDateFormat("EEE MM/dd/yy");
  public final static SimpleDateFormat USER_DATE_LONG = createSimpleDateFormat("EEE MM/dd/yyyy");
  public final static SimpleDateFormat USER_MONTH = createSimpleDateFormat("MMM yyyy");
  public final static SimpleDateFormat USER_MONTH_ONLY = createSimpleDateFormat("MMM");
  public final static SimpleDateFormat USER_DATETIME = createSimpleDateFormat("MM/dd/yyyy HH:mm");
  public final static SimpleDateFormat USER_DATETIME_SHORT = createSimpleDateFormat("MM/dd/yy HH:mm");
  public final static SimpleDateFormat USER_TIME = createSimpleDateFormat("HH:mm");
  public final static SimpleDateFormat USER_DAY_OF_WEEK = createSimpleDateFormat("EEE");
  public final static SimpleDateFormat USER_DOW_DATE = createSimpleDateFormat("EEE MM/dd/yyyy");
  public final static SimpleDateFormat USER_DOW_DATETIME_SHORT = createSimpleDateFormat(
      "EEE MM/dd/yy HH:mm");
  public final static SimpleDateFormat TIME = createSimpleDateFormat("HH:mm:ss");
  public final static SimpleDateFormat HOUR_MIN = createSimpleDateFormat("HH:mm");
  public final static SimpleDateFormat SHORT_DATE = createSimpleDateFormat("MM/dd/yy");
  public final static DecimalFormat DECIMAL = new DecimalFormat("###.##");
  //	public final static DecimalFormat CURRENCY = new DecimalFormat("$###,###.##;($###,###.##)");
  public final static NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);
  public final static NumberFormat NUMBER = NumberFormat.getInstance();
  public final static DecimalFormat PERCENT = new DecimalFormat("##.##%;(##.##%)");

  public static SimpleDateFormat createSimpleDateFormat(String pattern) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("PST"));
    return simpleDateFormat;
  }

  public final static long ONE_SEC = 1000;
  public final static long ONE_MIN = ONE_SEC * 60;
  public final static long ONE_HOUR = ONE_MIN * 60;


  /**
   * This method converts an input string into proper format by setting the first charater to
   * uppercase and all following characters into lowercase.  If the string contains a non letter
   * character, it will convert the trailing text into proper format as well.  This wethod will
   * typically be used for converting meta data column names into proper format.
   *
   * @return String The string in Proper format.
   */
  public static String printProper(String str) {
    StringBuilder builder = new StringBuilder();
    char prevCh = '.';   // The character that comes before ch in the string.
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      builder.append(Character.isLetter(ch) && !Character.isLetter(prevCh)
          ? Character.toUpperCase(ch) : Character.toLowerCase(ch));
      prevCh = ch;
    }

    return builder.toString();
  }

  public static String toSpaced(String camelCase) {
    StringBuilder out = new StringBuilder();
    char ch;       // One of the characters in str.
    if (camelCase.length() > 0) {
      out.append(Character.toUpperCase(camelCase.charAt(0)));
    }

    for (int i = 1; i < camelCase.length(); i++) {
      ch = camelCase.charAt(i);
      if (Character.isLetter(ch) && Character.isUpperCase(ch)) {
        out.append(" ");
      }
      out.append(ch);
    }
    return out.toString();
  }

  public static String toUnderScoreCase(String camelCase) {
    StringBuilder out = new StringBuilder();
    char ch;       // One of the characters in str.
    if (camelCase.length() > 0) {
      out.append(Character.toLowerCase(camelCase.charAt(0)));
    }

    for (int i = 1; i < camelCase.length(); i++) {
      ch = camelCase.charAt(i);
      if (Character.isLetter(ch) && Character.isUpperCase(ch)) {
        out.append("_");
      }
      out.append(Character.toLowerCase(ch));
    }
    return out.toString();
  }

  /**
   * This method converts an input string into proper format by setting the first charater to
   * uppercase, all following characters will be left as is.
   *
   * @return String The string in Capitalized format.
   */
  public static String toCapitalized(String str) {
    StringBuilder builder = new StringBuilder();
    char prevCh = '.';   // The character that comes before ch in the string.
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      builder.append(Character.isLetter(ch) && !Character.isLetter(prevCh) ? Character.toUpperCase(ch) : ch);
      prevCh = ch;
    }

    return builder.toString();
  }

  /**
   * This method converts an input string into all uppercase. Non-letter character will be left
   * as-is.
   *
   * @return String The string in CAPS format.
   */
  public static String printCaps(String str) {
    String letter = "";
    char ch;       // One of the characters in str.
    char prevCh;   // The character that comes before ch in the string.
    int i;         // A position in str, from 0 to str.length()-1.
    prevCh = '.';  // Prime the loop with any non-letter character.
    for (i = 0; i < str.length(); i++) {
      ch = str.charAt(i);
      if (Character.isLetter(ch) && !Character.isLetter(prevCh)) {
        letter += Character.toUpperCase(ch);
      } else {
        letter += Character.toUpperCase(ch);
      }
      prevCh = ch;
    }
    return letter;
  }

  /**
   * @param str
   * @return
   */
  public static String toLowerCamel(String str) {
    StringBuilder result = new StringBuilder();
    char ch;       // One of the characters in str.
    char prevCh = 'a';   // The character that comes before ch in the string.
    boolean foundNonLetter = false;
    for (int i = 0; i < str.length(); i++) {
      ch = str.charAt(i);
      if (Character.isLetter(ch) && !Character.isLetter(prevCh)) {
        result.append(Character.toUpperCase(ch));
      } else if (Character.isLetter(ch)) {
        result.append(Character.toLowerCase(ch));
      } else if (Character.isDigit(ch)) {
        result.append(ch);
      } else {
        foundNonLetter = true;
      }
      prevCh = ch;
    }

    if (!foundNonLetter) {
      return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    return result.toString();
  }

  /**
   * @param str
   * @return
   */
  public static String toUpperCamel(String str) {
    StringBuilder result = new StringBuilder();
    char ch;       // One of the characters in str.
    char prevCh;   // The character that comes before ch in the string.
    int i;         // A position in str, from 0 to str.length()-1.
    prevCh = '.';  // Prime the loop with any non-letter character.
    for (i = 0; i < str.length(); i++) {
      ch = str.charAt(i);
      if (Character.isLetter(ch)) {
        if (!Character.isLetter(prevCh)) {
          result.append(Character.toUpperCase(ch));
        } else {
          result.append(Character.toLowerCase(ch));
        }
      } else if (ch != ' ' && ch != '_' && ch != '.') {
        result.append(ch);
      }
      prevCh = ch;
    }

    return result.toString();
  }

  public static Calendar setCalendarDate(int day, int month, int year) {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.DAY_OF_MONTH, day);
    cal.set(Calendar.MONTH, month);
    cal.set(Calendar.YEAR, year);
    return cal;
  }

  public static int getCalendarDay(Calendar cal) {
    return cal.get(Calendar.DAY_OF_MONTH);
  }

  public static int getCalendarMonth(Calendar cal) {
    return cal.get(Calendar.MONTH) + 1;
  }

  public static int getCalendarYear(Calendar cal) {
    return cal.get(Calendar.YEAR);
  }

  public static int getCalendarWeekDay(Calendar cal) {
    return cal.get(Calendar.DAY_OF_WEEK);
  }

  public static String formatTime(Calendar cal) {
    return cal == null ? "00:00" : USER_TIME.format(cal.getTime());
  }

  public static String formatTime(DateTime dateTime) {
    return dateTime == null ? null : USER_TIME.format(dateTime.toDate());
  }

  public static String formatTime(long dateTime) {
    return formatTime(new DateTime(dateTime));
  }

  public static String formatTime(Instant instant) {
    return instant == null ? null : formatTime(instant.toEpochMilli());
  }

  public static String formatCurrency(double dollarAmount) {
    return CURRENCY.format(dollarAmount);
  }

  public static String cleanForHtml(String in) {
    if (in == null) {
      return "";
    }

    char i = (char) 10; //decimal 10 = ASCII \n

    if (in.contains(String.valueOf(i))) {
      return in.replace(i, ' ');
    }

    return in.replaceAll("'", "");
  }

  public static String formatDate(Calendar date, SimpleDateFormat dateformat) {
    return date == null ? null : dateformat.format(date.getTime());
  }

  public static String formatDate(Date date, SimpleDateFormat dateformat) {
    return date == null ? null : dateformat.format(date);
  }

  public static String formatDate(long date) {
    return formatDate(new DateTime(date));
  }

  public static String formatDate(Calendar date) {
    return formatDate(date, USER_DATETIME);
  }

  public static String formatDate(Date date) {
    return formatDate(date, USER_DATE);
  }

  public static Date parseDate(String value) throws ParseException {
    return parseDate(value, USER_DATE);
  }

  public static Date parseDate(String value, SimpleDateFormat format) throws ParseException {
    return value == null || value.length() == 0 ? null : format.parse(value);
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
    return dateTime == null ? null : formatDate(dateTime.toDate(), format);
  }

  public static String formatDate(Instant instant) {
    return formatDate(instant, USER_DATE);
  }

  public static String formatDate(Instant instant, SimpleDateFormat format) {
    return instant == null ? null : formatDate(new DateTime(instant.toEpochMilli()), format);
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

  public static String removeAccents(String input) {
    return Normalizer.normalize(input, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
  }
}
