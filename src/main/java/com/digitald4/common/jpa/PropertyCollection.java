package com.digitald4.common.jpa;

import com.digitald4.common.util.FormatText;
import com.digitald4.common.util.Pair;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.List;

public class PropertyCollection<T> extends PrimaryKey<Pair<String,Expression>> {
	
	private Hashtable<String, ValueCollection<T>> collections = new Hashtable<String, ValueCollection<T>>();
	private boolean complex;
	
	public PropertyCollection(boolean complex, Pair<String, Expression>... columns) {
		super(columns);
		this.complex = complex;
	}
	
	public List<T> getList(ValueCollection<T> crit) {
		ValueCollection<T> vc = collections.get(crit.getHashKey());
		return vc != null ? vc.getList() : null;
	}
	
	public boolean cache(T o) {
		Class<?> c = o.getClass();
		Object[] values = new Object[getKeys().length];
		int i = 0;
		for (Pair<String, Expression> key : getKeys()) {
			Object value = null;
			Method m = null;
			try {
				m = c.getMethod("get" + FormatText.toUpperCamel(key.getLeft()));
				value = m.invoke(o);
			} catch (NoSuchMethodException e) {
				try {
					m = c.getMethod("is" + FormatText.toUpperCamel(key.getLeft()));
					value = m.invoke(o);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				
			} catch (Exception e) {
				throw new RuntimeException("Error executing: " + m);
			}
			values[i++] = value;
		}
		//System.out.println("complex: " + isComplex());
		if (!isComplex()) {
			ValueCollection<T> crit = new ValueCollection<T>(this, values);
			List<T> collection = getList(crit);
			if (collection != null) {
				collection.add(o);
				return true;
			}
			return false;
		} else {
			boolean ret = false;
			for (ValueCollection<T> vc : collections.values()) {
				if (vc.meetsCriteria(values)) {
					vc.getList().add(o);
				}
			}
			return ret;
		}
	}
	
	public void evict(T o) {
		for (ValueCollection<T> vc : collections.values()) {
			vc.getList().remove(o);
		}
	}
	
	public boolean isComplex() {
		return complex;
	}
}
