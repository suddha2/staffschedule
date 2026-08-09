package com.midco.rota.service;

/**
 * Thrown when a new live solver session is requested but the configured cap on
 * concurrent live sessions is already reached (P5 resource guard). Surfaced as
 * HTTP 429 by the controller.
 */
public class LiveCapacityException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public LiveCapacityException(String message) {
		super(message);
	}
}
