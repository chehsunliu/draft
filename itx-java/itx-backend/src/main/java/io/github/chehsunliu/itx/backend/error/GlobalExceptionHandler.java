package io.github.chehsunliu.itx.backend.error;

import io.github.chehsunliu.itx.contract.queue.QueueException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BackendException.class)
  ResponseEntity<Map<String, Object>> handleBackend(BackendException e) {
    return body(e.getStatus(), e.getMessage());
  }

  @ExceptionHandler(QueueException.class)
  ResponseEntity<Map<String, Object>> handleQueue(QueueException e) {
    return body(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
  }

  @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
  ResponseEntity<Map<String, Object>> handleNoHandler(Exception e) {
    return body(HttpStatus.NOT_FOUND, "Not Found");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<Map<String, Object>> handleMethodNotAllowed(
      HttpRequestMethodNotSupportedException e) {
    return body(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String, Object>> handleAny(Exception e) {
    return body(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
  }

  private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(Map.of("message", message));
  }
}
