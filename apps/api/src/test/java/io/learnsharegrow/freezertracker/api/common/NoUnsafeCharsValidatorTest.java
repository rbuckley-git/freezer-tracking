package io.learnsharegrow.freezertracker.api.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NoUnsafeCharsValidatorTest {
  private final NoUnsafeCharsValidator validator = new NoUnsafeCharsValidator();

  @Test
  void shouldAllowSafeText() {
    assertTrue(validator.isValid("Frozen peas - shelf 2", null));
    assertTrue(validator.isValid("house@example.com", null));
  }

  @Test
  void shouldRejectMarkupCharacters() {
    assertFalse(validator.isValid("<script>", null));
    assertFalse(validator.isValid("quote\"test", null));
    assertFalse(validator.isValid("tick`test", null));
  }

  @Test
  void shouldRejectScriptSchemePatterns() {
    assertFalse(validator.isValid("javascript:alert(1)", null));
    assertFalse(validator.isValid("DATA:TEXT/HTML;base64,PHNjcmlwdA==", null));
  }

  @Test
  void shouldRejectControlCharacters() {
    assertFalse(validator.isValid("hello\u0000world", null));
  }
}
