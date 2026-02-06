package com.midco.rota;

import com.midco.rota.dto.ConflictError;

import java.util.List;

public class PinConflictException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = -573339480389349498L;
	private final List<ConflictError> conflicts;

    public PinConflictException(String message, List<ConflictError> conflicts) {
        super(message);
        this.conflicts = conflicts;
    }

    public List<ConflictError> getConflicts() {
        return conflicts;
    }
}