package io.learnsharegrow.freezertracker.api.common;

public class BadRequestException extends RuntimeException {
  public BadRequestException(String message) {
    super(message);
  }

  public static BadRequestException freezerName(String name) {
    return new BadRequestException("Freezer not found: " + name);
  }

  public static BadRequestException freezerId(Integer id) {
    return new BadRequestException("Freezer not found: " + id);
  }
}
