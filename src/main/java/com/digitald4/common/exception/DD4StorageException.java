package com.digitald4.common.exception;

public class DD4StorageException extends RuntimeException {

	private final int errorCode;

	public DD4StorageException(String message) {
		this(message, null, 500);
	}

	public DD4StorageException(String message, int errorCode) {
		this(message, null, errorCode);
	}

	public DD4StorageException(Exception e) {
		this(null, e, 500);
	}

	public DD4StorageException(Exception e, int errorCode) {
		this(null, e, errorCode);
	}

	public DD4StorageException(String message, Exception e) {
		this(message, e, 500);
	}

	public DD4StorageException(String message, Exception e, int errorCode) {
		super(message, e);
		this.errorCode = errorCode;
	}

	public int getErrorCode() {
		return errorCode;
	}
}
