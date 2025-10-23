package com.midco.rota;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CsvErrorHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleStreamingError(Exception ex) {
    	ex.printStackTrace();
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.TEXT_PLAIN)
            .body("An error occurred while generating the CSV."+ex.getMessage());
    }
}
