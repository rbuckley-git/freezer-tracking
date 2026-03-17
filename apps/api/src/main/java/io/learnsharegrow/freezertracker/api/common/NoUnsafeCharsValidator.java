package io.learnsharegrow.freezertracker.api.common;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoUnsafeCharsValidator implements ConstraintValidator<NoUnsafeChars, String> {
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }

    String normalised = value.toLowerCase(java.util.Locale.ROOT);
    if (normalised.contains("javascript:") || normalised.contains("data:text/html")) {
      return false;
    }

    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '<' || c == '>' || c == '"' || c == '\'' || c == '`') {
        return false;
      }
      if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
        return false;
      }
    }

    return true;
  }
}
