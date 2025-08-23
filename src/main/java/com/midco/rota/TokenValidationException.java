package com.midco.rota;

public class TokenValidationException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 8921223439510180004L;

	public TokenValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

