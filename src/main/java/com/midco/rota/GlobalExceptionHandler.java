package com.midco.rota;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PinConflictException.class)
    public ResponseEntity<Map<String, Object>> handlePinConflict(PinConflictException ex) {
        logger.error("Pin conflict validation failed: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("conflicts", ex.getConflicts());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}