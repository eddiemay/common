package com.digitald4.common.model;

import com.digitald4.common.dao.DataAccessObject;

public interface FileAttachable {
	
	public static final String DOWNLOAD_LINK = "<a href=\"download?classname=__className__&id=__id__\" class=\"document-pdf\" target=\"_blank\">"
			+ "<img src=\"images/icons/fugue/document-pdf.png\" width=\"16\" height=\"16\">Download</a>"
			+ "<a title=\"Delete\" href=\"#\" onClick=\"showDeleteDialog('__className__', __id__); return false;\">"
			+ "<img src=\"images/icons/fugue/cross-circle.png\" width=\"16\" height=\"16\"></a>";
	
	public FileAttachable setDataFile(DataFile df) throws Exception;
	public DataFile getDataFile();
	public DataAccessObject save() throws Exception;
}
