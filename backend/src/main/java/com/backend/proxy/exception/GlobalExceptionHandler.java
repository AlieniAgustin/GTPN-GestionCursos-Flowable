package com.backend.proxy.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FlowableSessionExpiredException.class)
    public ResponseEntity<ApiErrorResponse> handleFlowableSessionExpired(
            FlowableSessionExpiredException ex,
            HttpServletRequest request
    ) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        ApiErrorResponse error = new ApiErrorResponse(
            Instant.now(),
            HttpStatus.UNAUTHORIZED.value(),
            "FLOWABLE_SESSION_EXPIRED",
            ex.getMessage(),
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}
