package com.digitald4.common.exception;

public class DD4StorageException extends Exception {
	
	public DD4StorageException(String message) {
		super(message);
	}
	
	public DD4StorageException(String message, Exception e) {
		super(message, e);
	}
}
