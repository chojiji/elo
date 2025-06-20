package com.example.elo.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.awt.geom.RectangularShape;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> exceptionHandler(Exception ex){
        String errorMessage = ex.getMessage();
        return ResponseEntity.badRequest().body(errorMessage);
    }
}
