package com.digitald4.common.component;

import java.util.Collection;

import com.digitald4.common.model.GeneralData;
import com.digitald4.common.util.FormatText;

public class Column<T> {
	private final String name;
	private final String prop;
	private final Class<?> type;
	private final boolean editable;
	private final Collection<GeneralData> options;
	
	public Column(String name, String prop, Class<?> type, boolean editable) {
		this(name, prop, type, editable, null);
	}
	
	public Column(String name, String prop, Class<?> type, boolean editable, Collection<GeneralData> options) {
		this.name = name;
		this.prop = prop;
		this.type = type;
		this.editable = editable;
		this.options = options;
	}
	
	public String getName() {
		return name;
	}
	
	public String getProp() {
		return prop;
	}
	
	public String getMethodName() {
		return ((type == Boolean.class)? "is" : "get") + FormatText.toUpperCamel(getProp());
	}
	
	public Class<?> getType() {
		return type;
	}
	
	public boolean isEditable() {
		return editable;
	}
	
	public Collection<GeneralData> getOptions() {
		return options;
	}
	
	public Object getValue(T dao) throws Exception {
		return dao.getClass().getMethod(getMethodName()).invoke(dao);
	}
	
	public String getFieldId(Object id){
		return FormatText.toLowerCamel(getProp()) + (id != null ? id : "");
	}
}
