package io.learnsharegrow.freezertracker.api.common;

public class ForbiddenException extends RuntimeException {
  public ForbiddenException() {
    super("Forbidden");
  }
}
