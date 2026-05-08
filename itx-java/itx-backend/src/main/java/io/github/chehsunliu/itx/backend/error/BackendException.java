package io.github.chehsunliu.itx.backend.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BackendException extends RuntimeException {

  private final HttpStatus status;

  public BackendException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public static BackendException notFound() {
    return new BackendException(HttpStatus.NOT_FOUND, "not found");
  }

  public static BackendException badRequest(String message) {
    return new BackendException(HttpStatus.BAD_REQUEST, message);
  }

  public static BackendException unknown(String message) {
    return new BackendException(HttpStatus.INTERNAL_SERVER_ERROR, message);
  }
}
