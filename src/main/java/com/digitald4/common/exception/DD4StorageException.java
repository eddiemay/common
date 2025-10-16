package com.digitald4.common.exception;

public class DD4StorageException extends RuntimeException {
	public enum ErrorCode {
		BAD_REQUEST(400),
		NOT_AUTHENTICATED(401),
		FORBIDDEN(403),
		NOT_FOUND(404),
		CONFLICT(409),
		INTERNAL_SERVER_ERROR(500),
		NOT_IMPLEMENTED(501);

		private final int errorCode;
		ErrorCode(int errorCode) {
			this.errorCode = errorCode;
		}

		public int getErrorCode() {
			return errorCode;
		}
	}

	private final ErrorCode errorCode;

	public DD4StorageException(String message) {
		this(message, null, ErrorCode.INTERNAL_SERVER_ERROR);
	}

	public DD4StorageException(String message, ErrorCode errorCode) {
		this(message, null, errorCode);
	}

	public DD4StorageException(Exception e) {
		this(null, e, ErrorCode.INTERNAL_SERVER_ERROR);
	}

	public DD4StorageException(Exception e, ErrorCode errorCode) {
		this(null, e, errorCode);
	}

	public DD4StorageException(String message, Exception e) {
		this(message, e, ErrorCode.INTERNAL_SERVER_ERROR);
	}

	public DD4StorageException(String message, Exception e, ErrorCode errorCode) {
		super(message, e);
		this.errorCode = errorCode;
	}

	public int getErrorCode() {
		return errorCode.getErrorCode();
	}
}
