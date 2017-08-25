package com.digitald4.common.jpa;

import java.util.Calendar;

import com.digitald4.common.util.FormatText;

public class PrimaryKey<O> implements Entity, Comparable<Object> {
	
	private O[] keys;
	
	public PrimaryKey(O... keys) {
		this.keys = keys;
	}
	
	public O getKey(int index) {
		return keys[index];
	}
	
	public O[] getKeys() {
		return keys;
	}
	
	@Override
	public int hashCode() {
		return hashCode(keys);
	}
	
	public static int hashCode(Object... keys) {
		return getHashKey(keys).hashCode();
	}
	
	public String toString() {
		return getHashKey();
	}
	
	public String getHashKey() {
		return getHashKey(keys);
	}
	
  public static String getHashKey(Object... keys) {
  	String out="";
  	for (Object k:keys) {
  		if (out.length() > 0) {
  			out+="-";
  		}
  		out += getHashKey(k);
  	}
  	return out;
  }
  
	public static String getHashKey(Object o) {
		if (o == null) {
			return "0";
		}
		if (o instanceof Calendar) {
			return FormatText.formatDate((Calendar)o, FormatText.MYSQL_DATE);
		}
		return "" + o;
	}
	
	public int compareTo(Object o) {
		if (o == this) return 0;
		return toString().compareTo(o.toString());
	}
	
	public boolean equals(Object o) {
		if (o == this) return true;
		return toString().equals(o.toString());
	}
}
