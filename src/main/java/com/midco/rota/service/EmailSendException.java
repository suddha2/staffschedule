package com.midco.rota.service;

/** Thrown when a transactional email could not be delivered via Microsoft Graph. */
public class EmailSendException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public EmailSendException(String message) {
		super(message);
	}

	public EmailSendException(String message, Throwable cause) {
		super(message, cause);
	}
}
