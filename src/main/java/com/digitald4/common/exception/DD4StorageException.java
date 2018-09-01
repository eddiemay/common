package com.digitald4.common.exception;

import javax.servlet.http.HttpServletResponse;

public class DD4StorageException extends RuntimeException {

	private final int errorCode;

	public DD4StorageException(String message) {
		this(message, null, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}

	public DD4StorageException(String message, int errorCode) {
		this(message, null, errorCode);
	}

	public DD4StorageException(Exception e) {
		this(null, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}

	public DD4StorageException(String message, Exception e) {
		this(message, e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}

	public DD4StorageException(String message, Exception e, int errorCode) {
		super(message, e);
		this.errorCode = errorCode;
	}

	public int getErrorCode() {
		return errorCode;
	}
}
