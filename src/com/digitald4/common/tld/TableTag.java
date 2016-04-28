package com.digitald4.common.tld;

import java.util.Collection;

import org.joda.time.DateTime;

import com.digitald4.common.component.Column;
import com.digitald4.common.dao.DataAccessObject;
import com.digitald4.common.util.FormatText;

public class TableTag<T> extends DD4Tag {
	private final static String SECTION_START = "<section class=\"grid_12\" id=\"%tableId\">";
	private final static String START = "<div class=\"block-border\">"
			+ "<h1>%title</h1><table class=\"table sortable no-margin\" cellspacing=\"0\" width=\"100%\">";
	private final static String TITLE_START = "<thead><tr>"
			+ "<th class=\"black-cell\"><span class=\"loading\"></span></th>";
	private final static String TITLE_CELL = "<th scope=\"col\"><span class=\"column-sort\">"
			+ "<a href=\"#\" title=\"Sort up\" class=\"sort-up\"></a> <a href=\"#\" title=\"Sort down\" class=\"sort-down\"></a>"
			+ "</span> %colname</th>";
	private final static String TITLE_END = "</tr></thead><tbody>";
	private final static String ROW_START = "<tr id=\"%rowid\"><td class=\"th table-check-cell\"><input type=\"checkbox\" name=\"selected[]\" value=\"%id\"/></td>";
	private final static String CELL = "<td id=\"%propId\">%value</td>";
	private final static String ROW_END = "</tr>";
	private final static String END = "</tbody></table></div>";
	private final static String SECTION_END = "</section>";
	private String title;
	private Collection<Column<T>> columns;
	private Collection<T> data;
	private String callbackCode = "";

	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getTableId() {
		return FormatText.toLowerCamel(getTitle());
	}
	
	public void setColumns(Collection<Column<T>> columns) {
		this.columns = columns;
	}
	
	public Collection<Column<T>> getColumns() {
		return columns;
	}
	
	public void setData(Collection<T> data) {
		this.data = data;
	}
	
	public Collection<T> getData() {
		return data;
	}
	
	public void setCallbackCode(String callbackCode) {
		this.callbackCode = callbackCode;
	}
	
	public String getCallbackCode() {
		return callbackCode;
	}
	
	@Override
	public String getOutput() throws Exception {
		return SECTION_START.replace("%tableId", getTableId()) + getReplaceOutput() + SECTION_END;
	}
	
	public String getReplaceOutput() throws Exception {
		String out = START.replace("%title", getTitle());
		out += TITLE_START;
		for (Column<?> col : getColumns()) {
			out += TITLE_CELL.replace("%colname", col.getName());
		}
		out += TITLE_END;
		for (T dao : getData()) {
			int id;
			if (dao instanceof DataAccessObject) {
				id = ((DataAccessObject)dao).getId();
			} else {
				id = dao.hashCode();
			}
			out += ROW_START.replaceAll("%id", "" + id).replaceAll("%rowid", "row_" + dao.getClass().getSimpleName() + "_" + id);
			for (Column<T> col : getColumns()) {
				Object value = col.getValue(dao);
				if (value instanceof DateTime) {
					value = FormatText.formatDate((DateTime)value);
				}
				if (!col.isEditable()) {
					out += CELL.replace("%value", (value != null) ? "" + value : "").replaceAll("%propId", col.getFieldId(((DataAccessObject)dao).getId()));
				} else if (col.getOptions() == null) {
					InputTag input = new InputTag();
					input.setObject((DataAccessObject)dao);
					input.setProp(col.getProp());
					input.setValue(value);
					input.setAsync(true);
					input.setType(InputTag.Type.TEXT);
					input.setCallbackCode(getCallbackCode());
					out += "<td>" + input.getOutput() + "</td>";
				} else {
					InputTag input = new InputTag();
					input.setObject((DataAccessObject)dao);
					input.setProp(col.getProp());
					input.setValue(value);
					input.setAsync(true);
					input.setType(InputTag.Type.COMBO);
					input.setOptions(col.getOptions());
					input.setCallbackCode(getCallbackCode());
					out += "<td>" + input.getOutput() + "</td>";
				}
			}
			out += ROW_END;
		}
		out += END;
		return out;
	}
}
