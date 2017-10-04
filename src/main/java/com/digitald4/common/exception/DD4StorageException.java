package com.digitald4.common.exception;

public class DD4StorageException extends RuntimeException {

	private final int errorCode;

	public DD4StorageException(int errorCode, String message) {
		this(errorCode, message, null);
	}
	
	public DD4StorageException(String message) {
		this(500, message, null);
	}

	public DD4StorageException(int errorCode, Exception e) {
		this(errorCode, null, e);
	}

	public DD4StorageException(Exception e) {
		this(500, null, e);
	}

	public DD4StorageException(String message, Exception e) {
		this(500, message, e);
	}

	public DD4StorageException(int errorCode, String message, Exception e) {
		super(message, e);
		this.errorCode = errorCode;
	}

	public int getErrorCode() {
		return errorCode;
	}
}
