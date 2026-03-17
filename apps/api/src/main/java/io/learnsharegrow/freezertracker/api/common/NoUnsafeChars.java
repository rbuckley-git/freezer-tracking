package io.learnsharegrow.freezertracker.api.common;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Constraint(validatedBy = NoUnsafeCharsValidator.class)
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface NoUnsafeChars {
  String message() default "contains unsafe characters";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
