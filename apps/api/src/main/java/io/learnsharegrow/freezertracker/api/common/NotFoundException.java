package io.learnsharegrow.freezertracker.api.common;

import java.util.UUID;

public class NotFoundException extends RuntimeException {
  private NotFoundException(String message) {
    super(message);
  }

  public static NotFoundException item(UUID id) {
    return new NotFoundException("Item not found: " + id);
  }

  public static NotFoundException freezer(Integer id) {
    return new NotFoundException("Freezer not found: " + id);
  }

  public static NotFoundException account(UUID id) {
    return new NotFoundException("Account not found: " + id);
  }

  public static NotFoundException house(UUID id) {
    return new NotFoundException("House not found: " + id);
  }

  public static NotFoundException config(String key) {
    return new NotFoundException("Config not found: " + key);
  }
}
