package com.digitald4.common.exception;

public class DD4StorageException extends RuntimeException {
	
	public DD4StorageException(String message) {
		super(message);
	}
	
	public DD4StorageException(String message, Exception e) {
		super(message, e);
	}

	public DD4StorageException(Exception e) {
		super(e);
	}
}